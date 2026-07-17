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

    var observer: Observer!
    var delegate: Delegate!

    class ProductList {
        var products: [SKProduct]

        init() {
            products = []
        }
    }

    var productList: ProductList!

    @objc public func querySkuDetails(_ call: CAPPluginCall) {
        let productName = call.getString("product") ?? "fullversion"

        if productList == nil {
            productList = ProductList()
        }
        delegate = Delegate(call: call, self.productList)

        validate(productIdentifiers: [productName], call: call)
    }

    @objc public func launchBillingFlow(_ call: CAPPluginCall) {
        let productName = call.getString("product") ?? "fullversion"

        for product in self.productList.products {
            if product.productIdentifier == productName {
                let payment = SKMutablePayment(product: product)
                // Only a valid UUID round-trips into the App Store Server Notification appAccountToken field.
                if let token = call.getString("appAccountToken"), UUID(uuidString: token) != nil {
                    payment.applicationUsername = token.lowercased()
                }
                observer = Observer(call: call, product: productName)
                SKPaymentQueue.default().add(observer)
                SKPaymentQueue.default().add(payment)
            }
        }
    }

    var request: SKProductsRequest!

    func validate(productIdentifiers: [String], call: CAPPluginCall) {
         let productIdentifiers = Set(productIdentifiers)

         request = SKProductsRequest(productIdentifiers: productIdentifiers)

         request.delegate = delegate
         request.start()
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

                let transactionState: SKPaymentTransactionState = transaction.transactionState
                switch transactionState {
                    case .purchased:
                        if let appStoreReceiptURL = Bundle.main.appStoreReceiptURL,
                            FileManager.default.fileExists(atPath: appStoreReceiptURL.path) {

                            do {
                                let receiptData = try Data(contentsOf: appStoreReceiptURL, options: .alwaysMapped)

                                let receiptString = receiptData.base64EncodedString(options: [])
                                call?.resolve([
                                    "platform": "ios",
                                    "productId": self.product,
                                    "purchaseTime": Int64(NSDate().timeIntervalSince1970*1000),
                                    "storeKitTransactionID": transaction.transactionIdentifier ?? "N/A",
                                    "purchaseToken": receiptString,
                                ])
                            }
                            catch { call?.reject("no receipt")}
                        }
                    case .purchasing: break
                    case .failed: call?.reject("failed")
                    case .deferred: call?.reject("deferred")
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

    }

    public class Delegate: NSObject, SKProductsRequestDelegate {

        var call: CAPPluginCall?
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
                call?.resolve([
                   "price": product.price,
                   "price_currency_code": product.priceLocale.currencyCode!,
                   "title": product.localizedTitle,
                   "description": product.localizedDescription
               ])
            }

            for invalidIdentifier in response.invalidProductIdentifiers {
                print("invalid product id: \(invalidIdentifier)")
            }
        }
    }
}
