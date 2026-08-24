# billing

Capacitor plugin for in-app purchases (Android: Play Billing, iOS: StoreKit). The **host app** passes **product id** and **type** (`INAPP` / `SUBS`) on every call—the plugin is not tied to a specific SKU. (Native layers still apply fallback defaults if parameters are missing.)

The **`test-app/`** folder contains a small Capacitor 8 sample app so you can try **your own** store SKUs via the UI, `localStorage`, `.env.local` (`VITE_BILLING_TEST_*`), or `billingTestDefaults` in `test-app/capacitor.config.json` (see `test-app/README.md`).

The web platform is not a real store; the plugin returns stub values there—see below.

## iOS (Capacitor 8 + SPM)

From Capacitor 8 onward, the default iOS workflow uses **Swift Package Manager**. This plugin ships a root **`Package.swift`** so `npx cap sync` can link **`CapacitorBilling`** into `CapApp-SPM`. The native class conforms to **`CAPBridgedPlugin`** (same pattern as `@capacitor/haptics` and other first-party plugins).

Enable the **In-App Purchase** capability in Xcode for your app target.

## Android setup

### 1. Register the plugin (legacy snippet)

Auto-discovery usually registers plugins; if you still use manual registration in `MainActivity`:

```java
import de.carstenklaffke.billing.BillingPlugin;

this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
        add(BillingPlugin.class);
        }});
```

### 2. Gradle (Kotlin stdlib conflicts)

If your app hits duplicate Kotlin stdlib classes from Play Billing, add the following to your app’s `android/build.gradle` inside `allprojects` (or the equivalent in Gradle 8+), as needed:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
    }

    configurations.all {
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
    }
}
```

This plugin depends on Google Play Billing Library 7.x, which can pull older Kotlin stdlib artifacts. Excluding the deprecated `jdk7`/`jdk8` artifacts avoids duplicate classes with newer Capacitor / Kotlin toolchains.

Usage:

```javascript
import {BillingPlugin} from "capacitor-billing";
import {Device} from "@capacitor/device";

BillingPlugin.querySkuDetails({ product: 'YOUR_SKU', type: 'INAPP' }).then((result: any) => {
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

## Receipt validation

This plugin returns store tokens so **your backend** can verify the purchase. It does not call Apple or Google validation APIs itself (those need server secrets).

- **iOS:** `launchBillingFlow` resolves with `purchaseToken` (base64 App Store receipt) and `storeKitTransactionID`. Finish the StoreKit transaction with `finishTransaction`, then verify the receipt or the App Store Server API notification on your server.
- **Android:** `launchBillingFlow` resolves with Play’s purchase JSON, including `purchaseToken`. Acknowledge with `sendAck`, then verify the token with the [Google Play Developer API](https://developers.google.com/android-publisher) on your server.
