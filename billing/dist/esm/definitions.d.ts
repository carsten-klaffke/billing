declare module "@capacitor/core" {
    interface PluginRegistry {
        BillingPlugin: BillingPluginPlugin;
    }
}
export interface BillingPluginPlugin {
    querySkuDetails(): Promise<{
        value: string;
    }>;
    launchBillingFlow(): Promise<{
        value: string;
    }>;
}
