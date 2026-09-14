import Foundation
import Capacitor
import StoreKit

/**
 * Capacitor iOS billing (StoreKit 1). For SPM (Capacitor 8+), the class must conform to CAPBridgedPlugin.
 */
@objc(BillingPlugin)
public class BillingPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "BillingPlugin"
    public let jsName = "BillingPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "querySkuDetails", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "launchBillingFlow", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "finishTransaction", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendAck", returnType: CAPPluginReturnPromise)
    ]

    var observer: Observer?

    class ProductList {
        var products: [SKProduct]

        init() {
            products = []
        }
    }

    var productList: ProductList?
    /// SKProductsRequest.delegate is weak; keep request+delegate alive until StoreKit answers.
    private var inflightQueries: [SkuQuery] = []

    deinit {
        if let observer = observer {
            SKPaymentQueue.default().remove(observer)
        }
        for query in inflightQueries {
            query.request.cancel()
        }
    }

    private class SkuQuery {
        let request: SKProductsRequest
        let delegate: Delegate

        init(request: SKProductsRequest, delegate: Delegate) {
            self.request = request
            self.delegate = delegate
        }
    }

    @objc public func querySkuDetails(_ call: CAPPluginCall) {
        let productName = call.getString("product") ?? "fullversion"

        if productList == nil {
            productList = ProductList()
        }

        let delegate = Delegate(call: call, self.productList!)
        let request = SKProductsRequest(productIdentifiers: Set([productName]))
        let query = SkuQuery(request: request, delegate: delegate)
        delegate.onFinished = { [weak self, weak query] in
            guard let self = self, let query = query else { return }
            self.inflightQueries.removeAll { $0 === query }
        }
        request.delegate = delegate
        inflightQueries.append(query)
        request.start()
    }

    @objc public func launchBillingFlow(_ call: CAPPluginCall) {
        let productName = call.getString("product") ?? "fullversion"

        guard let productList = self.productList else {
            call.reject("No products loaded. Call querySkuDetails first.")
            return
        }

        guard let product = productList.products.first(where: { $0.productIdentifier == productName }) else {
            call.reject("Product not found: \(productName). Call querySkuDetails first.")
            return
        }

        let payment = SKMutablePayment(product: product)
        // Only a valid UUID round-trips into the App Store Server Notification appAccountToken field.
        if let token = call.getString("appAccountToken"), UUID(uuidString: token) != nil {
            payment.applicationUsername = token.lowercased()
        }
        if let existing = observer {
            existing.rejectOnce("Superseded by a new launchBillingFlow")
            SKPaymentQueue.default().remove(existing)
        }
        // Stale failed transactions for this SKU would otherwise fire immediately on the new observer.
        for transaction in SKPaymentQueue.default().transactions {
            if transaction.payment.productIdentifier == productName && transaction.transactionState == .failed {
                SKPaymentQueue.default().finishTransaction(transaction)
            }
        }
        let nextObserver = Observer(call: call, product: productName)
        observer = nextObserver
        SKPaymentQueue.default().add(nextObserver)
        SKPaymentQueue.default().add(payment)
    }

    @objc public func finishTransaction(_ call: CAPPluginCall) {
        guard let transactionId = call.getString("transactionId") else {
            call.reject("No transactionId provided")
            return
        }

        var foundTransaction: SKPaymentTransaction?

        for transaction in SKPaymentQueue.default().transactions {
            if transaction.transactionIdentifier == transactionId {
                foundTransaction = transaction
                break
            }
        }

        if let foundTransaction = foundTransaction {
            SKPaymentQueue.default().finishTransaction(foundTransaction)
            call.resolve()
        } else {
            call.reject("Transaction not found")
        }
    }

    /// Android-only acknowledge flow; on iOS use `finishTransaction` after purchase.
    @objc public func sendAck(_ call: CAPPluginCall) {
        call.reject("sendAck is not used on iOS; call finishTransaction with the StoreKit transaction id instead.")
    }

    public class Observer: NSObject, SKPaymentTransactionObserver {
        public func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
            for transaction in transactions {
                if transaction.payment.productIdentifier != self.product {
                    continue
                }

                let transactionState: SKPaymentTransactionState = transaction.transactionState
                switch transactionState {
                    case .purchased, .restored:
                        // Same payload as a new purchase so existing finishTransaction + backend
                        // validation still works. Do not auto-finish; the host app must call finishTransaction.
                        queue.remove(self)
                        settleWithReceipt(transaction)
                    case .purchasing: break
                    case .failed:
                        queue.finishTransaction(transaction)
                        queue.remove(self)
                        rejectOnce("failed")
                    case .deferred:
                        queue.remove(self)
                        rejectOnce("deferred")
                    @unknown default: print("Unexpected transaction state \(transaction.transactionState)")
                }
            }

        }
        var call: CAPPluginCall?
        init(call: CAPPluginCall, product: String) {
            self.call = call
            self.product = product
        }

        var product: String

        func settleWithReceipt(_ transaction: SKPaymentTransaction) {
            if let appStoreReceiptURL = Bundle.main.appStoreReceiptURL,
                FileManager.default.fileExists(atPath: appStoreReceiptURL.path) {
                do {
                    let receiptData = try Data(contentsOf: appStoreReceiptURL, options: .alwaysMapped)
                    let receiptString = receiptData.base64EncodedString(options: [])
                    resolveOnce([
                        "platform": "ios",
                        "productId": self.product,
                        "purchaseTime": Int64(NSDate().timeIntervalSince1970*1000),
                        "storeKitTransactionID": transaction.transactionIdentifier ?? "N/A",
                        "purchaseToken": receiptString,
                    ])
                } catch {
                    rejectOnce("no receipt")
                }
            } else {
                rejectOnce("no receipt")
            }
        }

        func resolveOnce(_ data: [String: Any]) {
            guard let call = call else { return }
            self.call = nil
            call.resolve(data)
        }

        func rejectOnce(_ message: String) {
            guard let call = call else { return }
            self.call = nil
            call.reject(message)
        }
    }

    public class Delegate: NSObject, SKProductsRequestDelegate {

        var call: CAPPluginCall?
        var onFinished: (() -> Void)?
        init(call: CAPPluginCall,_ productList: ProductList) {
            self.call = call
            self.productList = productList
        }

        var productList: ProductList

        public func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {

            if !response.products.isEmpty {
                let product = response.products[0]
                var contains = false
                for p in productList.products {
                    if product.productIdentifier == p.productIdentifier {
                        contains = true
                    }
                }
                if !contains {
                    productList.products.append(product)
                }
                settle { call in
                    call.resolve([
                       "price": product.price,
                       "price_currency_code": product.priceLocale.currencyCode ?? "",
                       "title": product.localizedTitle,
                       "description": product.localizedDescription
                   ])
                }
            } else {
                let invalid = response.invalidProductIdentifiers.joined(separator: ", ")
                if invalid.isEmpty {
                    settle { $0.reject("No products found") }
                } else {
                    settle { $0.reject("No products found: invalid product id(s) \(invalid)") }
                }
            }
        }

        public func request(_ request: SKRequest, didFailWithError error: Error) {
            settle { $0.reject("Product request failed: \(error.localizedDescription)") }
        }

        private func settle(_ block: (CAPPluginCall) -> Void) {
            guard let call = call else { return }
            self.call = nil
            block(call)
            onFinished?()
            onFinished = nil
        }
    }
}
