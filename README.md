# billing
Capacitor billing plugin

I implemented this Plugin to use in-App purchases in Ionic Apps with Capacitor as bridge. It is customized for a single product with name "fullversion", so you would have to adjust this for your purposes.

Web is not implemented, so check for "web" like below to handle the case. Android and iOS should open the corresponding stores.

Usage:

```javascript
import {BillingPlugin} from "capacitor-billing";
import {Device} from "@capacitor/device";

BillingPlugin.querySkuDetails().then((result: any) => {
    if (result) {
        if (result.value === "web") {
            setSkuInfos("web")
        } else {
            setSkuInfos({
                price: result.price,
                price_currency_code: result.price_currency_code,
                title: result.title,
                description: result.description
            });
        }
    } else {

    }
})

Device.getInfo().then((info: any) => {
    var product = "PRODUCT_NAME";
    BillingPlugin.launchBillingFlow({
        product: product,
        type: "SUBS"
    }).then((result: any) => {
        if (info.platform === "ios") {
            return BillingPlugin.finishTransaction({transactionId: result.storeKitTransactionID}).then((response: any) => {
                ...
                }
            )
        } else {
            return BillingPlugin.sendAck({purchaseToken: result.purchaseToken}).then((response: any) => {
                ...
                }
            )
        }
    })
})

```
Android:

Register in MainActivity.java
```java
import de.carstenklaffke.billing.BillingPlugin;

this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
        add(BillingPlugin.class);
        }});
```
iOS:

Add Capability for in-App purchases.
