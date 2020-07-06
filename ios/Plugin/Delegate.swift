import Foundation
import Capacitor

public class Delegate: SKProductsRequestDelegate {

    var call: CAPPluginCall?
    init(call: CAPPluginCall) {
        self.call = call
    }

    var products = [SKProduct]()
    // SKProductsRequestDelegate protocol method.
    func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        if !response.products.isEmpty {
           products = response.products
           // Custom method.
           print("received smth")
           let value = call.getString("value") ?? ""
           call.success([
               "value": value
           ])
        }

        for invalidIdentifier in response.invalidProductIdentifiers {
           // Handle any invalid product identifiers as appropriate.
        }
    }
}
