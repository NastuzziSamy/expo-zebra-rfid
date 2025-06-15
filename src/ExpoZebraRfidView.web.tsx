import * as React from 'react';

import { ExpoZebraRfidViewProps } from './ExpoZebraRfid.types';

export default function ExpoZebraRfidView(props: ExpoZebraRfidViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
