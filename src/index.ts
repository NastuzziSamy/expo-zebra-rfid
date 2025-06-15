// Reexport the native module. On web, it will be resolved to ExpoZebraRfidModule.web.ts
// and on native platforms to ExpoZebraRfidModule.ts
export { default } from './ExpoZebraRfidModule';
export { default as ExpoZebraRfidView } from './ExpoZebraRfidView';
export * from  './ExpoZebraRfid.types';
