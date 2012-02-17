/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alien_roger.android.ackdev.zxing;

import com.alien_roger.android.ackdev.zxing.common.BitMatrix;
import com.alien_roger.android.ackdev.zxing.oned.*;
import com.alien_roger.android.ackdev.zxing.qrcode.QRCodeWriter;

import java.util.Hashtable;

/**
 * This is a factory class which finds the appropriate Writer subclass for the BarcodeFormat
 * requested and encodes the barcode with the supplied contents.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class MultiFormatWriter implements Writer {

    public BitMatrix encode(String contents, BarcodeFormat format, int width,
                            int height) throws WriterException {

        return encode(contents, format, width, height, null);
    }

    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height,
                            Hashtable hints) throws WriterException {

        Writer writer;
        if (format == BarcodeFormat.EAN_8) {
            writer = new EAN8Writer();
        } else if (format == BarcodeFormat.EAN_13) {
            writer = new EAN13Writer();
        } else if (format == BarcodeFormat.UPC_A) {
            writer = new UPCAWriter();
        } else if (format == BarcodeFormat.QR_CODE) {
            writer = new QRCodeWriter();
        } else if (format == BarcodeFormat.CODE_39) {
            writer = new Code39Writer();
        } else if (format == BarcodeFormat.CODE_128) {
            writer = new Code128Writer();
        } else if (format == BarcodeFormat.ITF) {
            writer = new ITFWriter();
        } else {
            throw new IllegalArgumentException("No encoder available for format " + format);
        }
        return writer.encode(contents, format, width, height, hints);
    }

}
