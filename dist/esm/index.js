import { registerPlugin } from "@capacitor/core";
const BillingPlugin = registerPlugin("BillingPlugin", {
    web: () => import("./web").then((m) => new m.BillingPluginWeb()),
});
export * from "./definitions";
export { BillingPlugin };
//# sourceMappingURL=index.js.map