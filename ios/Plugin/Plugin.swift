import Foundation
import Capacitor
import StoreKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(BillingPlugin)
public class BillingPlugin: CAPPlugin {

    var delegate: Delegate!
    var observer: Observer!

    @objc func querySkuDetails(_ call: CAPPluginCall) {
        delegate = Delegate(call: call)
        validate(productIdentifiers: ["fullversion"], call: call)
    }

    @objc func launchBillingFlow(_ call: CAPPluginCall) {
        let payment = SKMutablePayment(product: delegate.product)
        SKPaymentQueue.default().add(observer)
        SKPaymentQueue.default().add(payment)
    }

    var request: SKProductsRequest!

    func validate(productIdentifiers: [String], call: CAPPluginCall) {
         let productIdentifiers = Set(productIdentifiers)

         request = SKProductsRequest(productIdentifiers: productIdentifiers)

         request.delegate = delegate
         request.start()
    }

    public class Observer: NSObject, SKPaymentTransactionObserver{
        public func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
            call?.success([
                "productId": "fullversion"
            ])
        }
        var call: CAPPluginCall?
        init(call: CAPPluginCall) {
            self.call = call
        }

    }

    public class Delegate: NSObject, SKProductsRequestDelegate {

        var call: CAPPluginCall?
        init(call: CAPPluginCall) {
            self.call = call
        }

        var product = SKProduct()
        // SKProductsRequestDelegate protocol method.
        public func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {

            if !response.products.isEmpty {
                product = response.products[0]
                call?.success([
                   "price": product.price,
                   "price_locale": product.priceLocale,
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
