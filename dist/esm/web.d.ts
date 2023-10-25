import { WebPlugin } from '@capacitor/core';
import { BillingPluginPlugin } from './definitions';
export declare class BillingPluginWeb extends WebPlugin implements BillingPluginPlugin {
    constructor();
    querySkuDetails(options: {
        product: string;
        type: string;
    }): Promise<{
        value: string;
    }>;
    launchBillingFlow(options: {
        product: string;
        type: string;
    }): Promise<{
        value: string;
    }>;
    sendAck(options: {
        purchaseToken: string;
    }): Promise<{
        value: string;
    }>;
    finishTransaction(options: {
        transactionId: string;
    }): Promise<{
        value: string;
    }>;
}
