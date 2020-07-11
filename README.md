# billing
Capacitor billing plugin

I implemented this Plugin to use in-App purchases in Ionic Apps with Capacitor as bridge. It is customized for a single product with name "fullversion", so you would have to adjust this for your purposes.

Web is not implemented, so check for "web" like below to handle the case. Android and iOS should open the corresponding stores.

Usage:

```javascript
import {Plugins} from '@capacitor/core';

Plugins.BillingPlugin.querySkuDetails().then((result: any) => {
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

 Plugins.BillingPlugin.launchBillingFlow().then((result: any) => {
     createPurchase(result).then(purchase => {
         //success
     })
 }).catch(() => {
     
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
