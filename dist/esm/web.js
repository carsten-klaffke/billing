import { WebPlugin } from '@capacitor/core';
export class BillingPluginWeb extends WebPlugin {
    constructor() {
        super();
    }
    // @ts-ignore
    async querySkuDetails(options) {
        return { value: "web" };
    }
    // @ts-ignore
    async launchBillingFlow(options) {
        return { value: "web" };
    }
    // @ts-ignore
    async sendAck(options) {
        return { value: "web" };
    }
    // @ts-ignore
    async finishTransaction(options) {
        return { value: "web" };
    }
}
//# sourceMappingURL=web.js.map