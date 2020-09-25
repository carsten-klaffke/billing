var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { WebPlugin } from '@capacitor/core';
export class BillingPluginWeb extends WebPlugin {
    constructor() {
        super({
            name: 'BillingPlugin',
            platforms: ['web']
        });
    }
    // @ts-ignore
    querySkuDetails(options) {
        return __awaiter(this, void 0, void 0, function* () {
            return { value: "web" };
        });
    }
    // @ts-ignore
    launchBillingFlow(options) {
        return __awaiter(this, void 0, void 0, function* () {
            return { value: "web" };
        });
    }
    // @ts-ignore
    sendAck(options) {
        return __awaiter(this, void 0, void 0, function* () {
            return { value: "web" };
        });
    }
}
const BillingPlugin = new BillingPluginWeb();
export { BillingPlugin };
import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(BillingPlugin);
//# sourceMappingURL=web.js.map