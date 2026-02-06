import { NativeModules } from 'react-native';

export interface PrintTextStyle {
  textSize?: number;
  underline?: boolean;
  textScaleX?: number;
  textScaleY?: number;
  letterSpacing?: number;
  lineSpacing?: number;
  topPadding?: number;
  leftPadding?: number;
  align?: PrintAlign;
  font?: string;
}

export enum PrintAlign {
  LEFT,
  CENTER,
  RIGHT,
}

export enum BarcodeTextPosition {
  NO_TEXT,
  TEXT_ABOVE,
  TEXT_BELOW,
  BOTH,
}

export enum BitmapType {
  /// for black and white bitmap
  BLACK_WHITE,

  /// grayscale bitmap, suitable for rich color images
  GRAYSCALE,
}

export enum LcdOpt {
  INIT,
  WAKEUP,
  SLEEP,
  CLEAR,
  RESET,
}

export enum PrinterStatus {
  SDK_OK = 0,
  PRN_BASE_ERR = -1200,
  PRN_COVER_OPEN = PRN_BASE_ERR - 1,
  PRN_PARAM_ERR = PRN_BASE_ERR - 2,
  PRN_NO_PAPER = PRN_BASE_ERR - 3,
  PRN_OVERHEAT = PRN_BASE_ERR - 4,
  PRN_UNKNOWN_ERR = PRN_BASE_ERR - 5,
  PRN_PRINTING = PRN_BASE_ERR - 6,
  PRN_NO_NFC = PRN_BASE_ERR - 7,
  PRN_NFC_NO_PAPER = PRN_BASE_ERR - 8,
  PRN_LOW_BATTERY = PRN_BASE_ERR - 9,
}

export namespace PrinterStatus {
  export function msg(code: number) {
    switch (code) {
      case PrinterStatus.SDK_OK:
        return 'Success';
      case PrinterStatus.PRN_COVER_OPEN:
        return 'Printer cover open';
      case PrinterStatus.PRN_PARAM_ERR:
        return 'Printer params error';
      case PrinterStatus.PRN_NO_PAPER:
        return 'Printer no paper';
      case PrinterStatus.PRN_OVERHEAT:
        return 'Printer overheat';
      case PrinterStatus.PRN_UNKNOWN_ERR:
        return 'Printer unknown error';
      case PrinterStatus.PRN_PRINTING:
        return 'Printer is printing';
      case PrinterStatus.PRN_NO_NFC:
        return 'Printer no NFC';
      case PrinterStatus.PRN_NFC_NO_PAPER:
        return 'Printer NFC no paper';
      case PrinterStatus.PRN_LOW_BATTERY:
        return 'Printer low battery';
      default:
        return 'Unknown error';
    }
  }
}

export interface ScannerOptions {
  // Action bar title in scanner capture page
  title?: string;
  // Whether to display the album button, true by default
  showAlbum?: boolean;
  // Vibrate when scan completes, true by default
  playVibrate?: boolean;
  // Sound prompt after scan completes, true by default
  playSound?: boolean;
}

type NyxPrinterType = {
  getServiceVersion(): Promise<string>;
  getPrinterVersion(): Promise<string>;
  getPrinterStatus(): Promise<number>;
  paperOut(px: number): Promise<void>;
  paperBack(px: number): Promise<void>;
  printText(text: string, textStyle: PrintTextStyle): Promise<void>;
  printText2(text: string, textStyle: PrintTextStyle, textWidth: number, align: PrintAlign): Promise<void>;
  printBarcode(data: string, width: number, height: number, textPosition: BarcodeTextPosition, align: PrintAlign): Promise<void>;
  printQrCode(data: string, width: number, height: number, align: PrintAlign): Promise<void>;
  printBitmap(base64Data: string, type: BitmapType, align: PrintAlign): Promise<void>;
  printRasterData(base64Data: string): Promise<void>;
  printEscposData(base64Data: string): Promise<void>;
  printTableText(texts: string[], weights: number[], styles: PrintTextStyle[]): Promise<void>;
  printEndAutoOut(): Promise<void>;
  labelLocate(labelHeight: number, labelGap: number): Promise<void>;
  labelPrintEnd(): Promise<void>;
  labelLocateAuto(): Promise<void>;
  labelDetectAuto(): Promise<void>;
  hasLabelLearning(): Promise<boolean>;
  clearLabelLearning(): Promise<void>;
  configLcd(opt: LcdOpt): Promise<void>;
  showLcdBitmap(base64Data: string): Promise<void>;
  openCashBox(): Promise<void>;
  cameraScan(opt: ScannerOptions): Promise<void>;
  qscScan(): Promise<void>;
};

const { NyxPrinter } = NativeModules;

export default NyxPrinter as NyxPrinterType;
