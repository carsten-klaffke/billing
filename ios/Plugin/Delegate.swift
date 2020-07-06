import Foundation
import Capacitor
import StoreKit

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
