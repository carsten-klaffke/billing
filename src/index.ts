import { registerPlugin } from "@capacitor/core";
import type { BillingPluginPlugin } from "./definitions";

const BillingPlugin = registerPlugin<BillingPluginPlugin>("BillingPlugin", {
    web: () => import("./web").then((m) => new m.BillingPluginWeb()),
});

export * from "./definitions";
export { BillingPlugin };