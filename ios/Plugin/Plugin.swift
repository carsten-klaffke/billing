import Foundation
import Capacitor
import StoreKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(BillingPlugin)
public class BillingPlugin: CAPPlugin {

    @objc func querySkuDetails(_ call: CAPPluginCall) {
        validate(productIdentifiers: ["mindlib-full-version"], call: call)
        let value = call.getString("value") ?? ""
        call.success([
            "value": value
        ])
    }

    @objc func launchBillingFlow(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.success([
            "value": value
        ])
    }

    var request: SKProductsRequest!

    func validate(productIdentifiers: [String], call: CAPPluginCall) {
         let productIdentifiers = Set(productIdentifiers)

         request = SKProductsRequest(productIdentifiers: productIdentifiers)
        request.delegate = Delegate(call: call) as SKProductsRequestDelegate
         request.start()
    }

    public class Delegate: NSObject, SKProductsRequestDelegate {

        var call: CAPPluginCall?
        init(call: CAPPluginCall) {
            self.call = call
        }

        var products = [SKProduct]()
        // SKProductsRequestDelegate protocol method.
        public func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
            if !response.products.isEmpty {
               products = response.products
               // Custom method.
               print("received smth")
                call?.success([
                   "value": "success"
               ])
            }

            for invalidIdentifier in response.invalidProductIdentifiers {
               // Handle any invalid product identifiers as appropriate.
            }
        }
    }
}
