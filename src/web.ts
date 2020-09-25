import { WebPlugin } from '@capacitor/core';
import { BillingPluginPlugin } from './definitions';

export class BillingPluginWeb extends WebPlugin implements BillingPluginPlugin {
  constructor() {
    super({
      name: 'BillingPlugin',
      platforms: ['web']
    });
  }

  // @ts-ignore
  async querySkuDetails(options: {product: string, type: string}): Promise<{value: string}> {
    return {value: "web"};
  }

  // @ts-ignore
  async launchBillingFlow(options: {product: string, type: string}): Promise<{value: string}> {
    return {value: "web"};
  }

  // @ts-ignore
  async sendAck(options: {purchaseToken: string}): Promise<{value: string}> {
    return {value: "web"};
  }

}

const BillingPlugin = new BillingPluginWeb();

export { BillingPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(BillingPlugin);
