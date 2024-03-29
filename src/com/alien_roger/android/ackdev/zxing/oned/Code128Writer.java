/*
 * Copyright 2010 ZXing authors
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

package com.alien_roger.android.ackdev.zxing.oned;

import com.alien_roger.android.ackdev.zxing.BarcodeFormat;
import com.alien_roger.android.ackdev.zxing.WriterException;
import com.alien_roger.android.ackdev.zxing.common.BitMatrix;

import java.util.Hashtable;

/**
 * This object renders a CODE128 code as a {@link BitMatrix}.
 *
 * @author erik.barbara@gmail.com (Erik Barbara)
 */
public final class Code128Writer extends UPCEANWriter {

    public BitMatrix encode(String contents,
                            BarcodeFormat format,
                            int width,
                            int height,
                            Hashtable hints) throws WriterException {
        if (format != BarcodeFormat.CODE_128) {
            throw new IllegalArgumentException("Can only encode CODE_128, but got " + format);
        }
        return super.encode(contents, format, width, height, hints);
    }

    public byte[] encode(String contents) {
        int length = contents.length();
        if (length > 80) {
            throw new IllegalArgumentException(
                    "Requested contents should be less than 80 digits long, but got " + length);
        }

        //Determine which code we should use (C or B)
        boolean useCodeC = true;
        for (int i = 0; i < length; i++) {
            char c = contents.charAt(i);
            if (c < '0' || c > '9') {
                useCodeC = false;
                break;
            }
        }

        int codeWidth = 11 + 11 + 13; //start plus check plus stop character
        byte[] result;
        int pos;
        int check;

        if (useCodeC) {
            //Optionnaly add "0" to have pairs
            if (length % 2 != 0) {
                contents = '0' + contents;
                length++;
            }
            //get total code width for this barcode
            for (int i = 0; i < length; i += 2) {
                int[] patterns = Code128Reader.CODE_PATTERNS[Integer.parseInt(contents.substring(i, i + 2))];
                for (int j = 0; j < patterns.length; j++) {
                    codeWidth += patterns[j];
                }
            }
            result = new byte[codeWidth];
            pos = appendPattern(result, 0, Code128Reader.CODE_PATTERNS[105], 1);
            check = 105;
            //append next character to bytematrix
            for (int i = 0; i < length; i += 2) {
                int pair = Integer.parseInt(contents.substring(i, i + 2));
                check += pair * (i / 2 + 1);
                pos += appendPattern(result, pos, Code128Reader.CODE_PATTERNS[pair], 1);
            }
        } else {
            //get total code width for this barcode
            for (int i = 0; i < length; i++) {
                int[] patterns = Code128Reader.CODE_PATTERNS[contents.charAt(i) - ' '];
                for (int j = 0; j < patterns.length; j++) {
                    codeWidth += patterns[j];
                }
            }
            result = new byte[codeWidth];
            pos = appendPattern(result, 0, Code128Reader.CODE_PATTERNS[104], 1);
            check = 104;
            //append next character to bytematrix
            for (int i = 0; i < length; i++) {
                check += (contents.charAt(i) - ' ') * (i + 1);
                pos += appendPattern(result, pos, Code128Reader.CODE_PATTERNS[contents.charAt(i) - ' '], 1);
            }
        }

        //compute checksum and append it along with end character and quiet space
        check %= 103;
        pos += appendPattern(result, pos, Code128Reader.CODE_PATTERNS[check], 1);
        pos += appendPattern(result, pos, Code128Reader.CODE_PATTERNS[106], 1);

        return result;
    }

}
