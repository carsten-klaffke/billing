import Foundation
import Capacitor
import StoreKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(BillingPlugin)
public class BillingPlugin: CAPPlugin {

    var observer: Observer!
    var delegate: Delegate!

    class ProductList {
        var products: [SKProduct]

        init() {
            products = []
        }
    }

    var productList: ProductList!

    @objc func querySkuDetails(_ call: CAPPluginCall) {
        let productName = call.getString("product") ?? "fullversion"

        if(productList == nil){
            productList = ProductList()
        }
        delegate = Delegate(call: call, self.productList)

        validate(productIdentifiers: [productName], call: call)
    }

    @objc func launchBillingFlow(_ call: CAPPluginCall) {
        let productName = call.getString("product") ?? "fullversion"

        for product in self.productList.products {
            if(product.productIdentifier == productName){
                let payment = SKMutablePayment(product: product)
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

    @objc func finishTransaction(_ call: CAPPluginCall) {
        guard let transactionId = call.getString("transactionId") else {
            call.reject("No transactionId provided")
            return
        }

        var foundTransaction: SKPaymentTransaction? = nil

        for transaction in SKPaymentQueue.default().transactions {
            if transaction.transactionIdentifier == transactionId {
                foundTransaction = transaction
                break
            }
        }

        if let foundTransaction = foundTransaction {
            print(foundTransaction)
            SKPaymentQueue.default().finishTransaction(foundTransaction)
            call.success()
        } else {
            call.reject("Transaction not found")
        }
    }

    public class Observer: NSObject, SKPaymentTransactionObserver{
        public func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
            for transaction in transactions {

                let transactionState: SKPaymentTransactionState = transaction.transactionState
                switch transactionState {
                    case .purchased:
                        // Get the receipt if it's available
                        if let appStoreReceiptURL = Bundle.main.appStoreReceiptURL,
                            FileManager.default.fileExists(atPath: appStoreReceiptURL.path) {

                            do {
                                let receiptData = try Data(contentsOf: appStoreReceiptURL, options: .alwaysMapped)

                                let receiptString = receiptData.base64EncodedString(options: [])
                                call?.success([
                                    "platform": "ios",
                                    "productId": self.product,
                                    "purchaseTime": Int64(NSDate().timeIntervalSince1970*1000),
                                    "storeKitTransactionID": transaction.transactionIdentifier ?? "N/A",
                                    "purchaseToken": receiptString,
                                ])
                            }
                            catch { call?.error("no receipt")}
                        }
                    case .purchasing: break
                    case .failed: call?.error("failed")
                    case .deferred: call?.error("deferred")
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
        // SKProductsRequestDelegate protocol method.
        public func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {

            if !response.products.isEmpty {
                let product = response.products[0]
                var contains = false;
                for p in productList.products {
                    if(product.productIdentifier == p.productIdentifier){
                        contains = true
                    }
                }
                if(!contains){
                    productList.products.append(product)
                }
                call?.success([
                   "price": product.price,
                   "price_currency_code": product.priceLocale.currencyCode!,
                   "title": product.localizedTitle,
                   "description": product.localizedDescription
               ])
            }

            for invalidIdentifier in response.invalidProductIdentifiers {
               // Handle any invalid product identifiers as appropriate.
                print("invalid")
                print(invalidIdentifier)
            }
        }
    }
}
