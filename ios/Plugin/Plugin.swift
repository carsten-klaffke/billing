import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(BillingPlugin)
public class BillingPlugin: CAPPlugin {
    
    @objc func querySkuDetails(_ call: CAPPluginCall) {
        validate(productIdentifiers: ["fullversion"])
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
         request.delegate = Delegate(call: call)
         request.start()
    }


}
