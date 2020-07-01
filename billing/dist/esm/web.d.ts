import { WebPlugin } from '@capacitor/core';
import { BillingPluginPlugin } from './definitions';
export declare class BillingPluginWeb extends WebPlugin implements BillingPluginPlugin {
    constructor();
    querySkuDetails(): Promise<{
        value: string;
    }>;
    launchBillingFlow(): Promise<{
        value: string;
    }>;
}
declare const BillingPlugin: BillingPluginWeb;
export { BillingPlugin };
