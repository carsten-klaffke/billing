import { WebPlugin } from '@capacitor/core';
import { BillingPluginPlugin } from './definitions';

export class BillingPluginWeb extends WebPlugin implements BillingPluginPlugin {
  constructor() {
    super({
      name: 'BillingPlugin',
      platforms: ['web']
    });
  }

  async querySkuDetails(): Promise<{value: string}> {
    return {value: "web"};
  }

  async launchBillingFlow(): Promise<{value: string}> {
    return {value: "web"};
  }

}

const BillingPlugin = new BillingPluginWeb();

export { BillingPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(BillingPlugin);
