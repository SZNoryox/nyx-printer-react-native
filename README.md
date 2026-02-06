# nyx-printer-react-native

React native plugin for nyx printer service

## Installation

```sh
npm install nyx-printer-react-native
```

## Usage

```js
import NyxPrinter from "nyx-printer-react-native";

// ...
```

### Printer
```js
const _printTest = async () => {
  try {
    let ret = await NyxPrinter.getPrinterStatus();
    if (ret != PrinterStatus.SDK_OK) {
    appendLog(`printer status: ${PrinterStatus.msg(ret)}`);
    return
    }
    await NyxPrinter.printText("Receipt", { textSize: 48, align: PrintAlign.CENTER });
    await NyxPrinter.printText(`\nOrder Time:\t${Date.now()}\n`, { align: PrintAlign.CENTER });
    let weights = [1, 1, 1, 1];
    let row1 = ["ITEM", "QTY", "PRICE", "TOTAL"];
    let row2 = ["Apple", "1", "2.00", "2.00"];
    let row3 = ["Orange", "1", "2.00", "2.00"];
    let row4 = ["Banana", "1", "2.00", "2.00"];
    let row5 = ["Cherry", "1", "2.00", "2.00"];
    let styles = [
    { align: PrintAlign.CENTER },
    { align: PrintAlign.CENTER },
    { align: PrintAlign.CENTER },
    { align: PrintAlign.CENTER }
    ];
    await NyxPrinter.printTableText(row1, weights, styles);
    await NyxPrinter.printTableText(row2, weights, styles);
    await NyxPrinter.printTableText(row3, weights, styles);
    await NyxPrinter.printTableText(row4, weights, styles);
    await NyxPrinter.printTableText(row5, weights, styles);
    await NyxPrinter.printText("\nOrder Price: \t\t9999.00\n", { align: PrintAlign.CENTER });
    await NyxPrinter.printQrCode(Date.now().toString(), 300, 300, PrintAlign.CENTER);
    await NyxPrinter.printText("\n", {});
    await NyxPrinter.printBarcode(Date.now().toString(), 300, 150, BarcodeTextPosition.TEXT_BELOW, PrintAlign.CENTER);
    await NyxPrinter.printBitmap(imageBase64, BitmapType.BLACK_WHITE, PrintAlign.CENTER);
    await NyxPrinter.printText("\n***Print Complete***", { align: PrintAlign.CENTER });
    await NyxPrinter.printEndAutoOut();
  } catch (e) {
      appendLog(`printTest: ${e}`);
  }
};
```

### LCD
```js
const _showLcd = async () => {
  try {
    await NyxPrinter.configLcd(LcdOpt.INIT);
    await NyxPrinter.showLcdBitmap(imageBase64);
  } catch (e) {
      appendLog(`showLcd: ${e}`);
  }
};

const _resetLcd = async () => {
  try {
    await NyxPrinter.configLcd(LcdOpt.INIT);
    await NyxPrinter.configLcd(LcdOpt.RESET);
  } catch (e) {
      appendLog(`resetLcd: ${e}`);
  }
};

const _wakeupLcd = async () => {
  try {
    await NyxPrinter.configLcd(LcdOpt.INIT);
    await NyxPrinter.configLcd(LcdOpt.WAKEUP);
  } catch (e) {
      appendLog(`wakeupLcd: ${e}`);
  }
};

const _sleepLcd = async () => {
  try {
    await NyxPrinter.configLcd(LcdOpt.INIT);
    await NyxPrinter.configLcd(LcdOpt.SLEEP);
  } catch (e) {
      appendLog(`resetLcd: ${e}`);
  }
};
```

### Scanner
- Register scanner result listener
```js
React.useEffect(() => {
  // register scanner listener
  DeviceEventEmitter.addListener('onScanResult', (res) => {
    appendLog(`scan result: ${JSON.stringify(res)}`);
  });
  return () => DeviceEventEmitter.removeAllListeners('onScanResult');
}, []);
```

- Camera scan
```js
const _cameraScan = async () => {
  try {
    await NyxPrinter.cameraScan({});
  } catch (e) {
    appendLog(`cameraScan: ${e}`);
  }
};
```

- Infrared scan: By default, infrared scan will be triggered by the side button. Here is the code for soft tirgger
```js
const _infraredScan = async () => {
  try {
    await NyxPrinter.qscScan();
  } catch (e) {
    appendLog(`infraredScan: ${e}`);
  }
};
```

### Cash Box
```js
const _openCashBox = async () => {
  try {
    await NyxPrinter.openCashBox();
  } catch (e) {
    appendLog(`openCashBox: ${e}`);
  }
};
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
