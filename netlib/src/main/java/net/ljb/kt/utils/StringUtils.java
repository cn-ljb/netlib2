package net.ljb.kt.utils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 字符串转换，使用包装类
 * Created by zhangzhang on 16/1/21.
 */
public class StringUtils {
    public static int String2Int(String data) {
        int result = -1;

        try {
            result = Integer.valueOf(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 整形转换
     *
     * @param data 输入
     * @return Integer
     */
    public static Integer toInt(String data) {
        Integer result = 0;

        try {
            result = TextUtils.isEmpty(data)?0:Integer.valueOf(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Long转换
     *
     * @param data 输入
     * @return Long
     */
    public static Long toLong(String data) {
        Long result = 0L;

        try {
            result = TextUtils.isEmpty(data)?0:Long.valueOf(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 浮点转换
     *
     * @param data 输入
     * @return Float
     */
    public static Float toFloat(String data) {
        Float result = 0.0f;

        if (data != null && data.length() > 0) {
            try {
                result = TextUtils.isEmpty(data)?0:Float.valueOf(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 浮点转换
     *
     * @param data 输入
     * @return Double
     */
    public static Double toDouble(String data) {
        Double result = 0.0;

        try {
            result = TextUtils.isEmpty(data)?0:Double.valueOf(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 判断是否是有效的值，0和0.0均视为false
     *
     * @param data 输入
     * @return boolean
     */
    public static boolean isEmpty(String data) {
        String tmp = (data != null ? data.replaceAll(" ", "") : "");

        if (TextUtils.isEmpty(tmp)) {
            return true;
        } else {
            try {
                return Double.valueOf(tmp) < 0.00001;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * 字符串截取
     *
     * @param in  原始数据
     * @param max 最大文字截取尺寸
     */
    public static String subString(String in, int max) {
        float j = 0;  // 半角数目
        int offset = 0;

        if (max == -1) {
            max = Integer.MAX_VALUE;
        }

        if (in != null && max > 0) {
            for (offset = 0; offset < in.codePointCount(0, in.length()); offset++) {
                int c = in.codePointAt(offset);

                if (j > max) {
                    break;
                } else {
                    if (c > 32 && c <= 127) {
                        j += 0.5;
                    } else if (c == 10 || c == 13) {
                        break;
                    } else {
                        j += 1;
                    }
                }
            }
        }

        if (in != null) {
            offset = offset > in.length() ? in.length() : offset;
            return in.substring(0, offset);
        } else {
            return "";
        }
    }

    /**
     * 字符串截取
     *
     * @param in           原始数据
     * @param maxLine      做多行数
     * @param eachLineSize 每行字数
     */
    public static String[] subString(String in, int maxLine, int eachLineSize) {
        String inputString = in;
        ArrayList<String> arrayList = new ArrayList<>();

        for (int i = 0; i < maxLine; i++) {
            String tmp = subString(inputString, eachLineSize);

            if (i == maxLine - 1 && i > 0) {
                tmp += ".";
            }

            arrayList.add(tmp);

            inputString = inputString.substring(Math.min(inputString.length(), tmp.length()),
                    inputString.length());
            if (inputString.startsWith("\r\n")) {
                inputString = inputString.substring(2, inputString.length());
            } else if (inputString.startsWith("\r")) {
                inputString = inputString.substring(1, inputString.length());
            } else if (inputString.startsWith("\n")) {
                inputString = inputString.substring(1, inputString.length());
            }

            if (inputString.length() == 0) {
                break;
            }
        }

        String[] ret = new String[arrayList.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = arrayList.get(i);
        }

        return ret;
    }

    public static String getString(String s) {
        if (TextUtils.isEmpty(s)) {
            return "";
        }
        return s;
    }


    public static String listToString(List<String> stringList) {
        StringBuilder content = new StringBuilder();
        if (stringList != null && stringList.size() > 0) {
            for (int i = 0; i < stringList.size(); i++) {
                if (!TextUtils.isEmpty(stringList.get(i))) {
                    content.append(stringList.get(i));
                    content.append(",");
                }
            }
        } else {
            return "";
        }
        return content.toString().length() > 0 ? content.toString().substring(0, content.length() - 1) : "";
    }

    /**
     * unicode 转换  显示中文
     * @param unicodeStr
     * @return
     */
    public static String unicodeToString(String unicodeStr) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(unicodeStr);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            unicodeStr = unicodeStr.replace(matcher.group(1), ch + "");
        }
        return unicodeStr;
    }

    public static String decode(String unicode) {
        char[] chars = unicode.toCharArray();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0, len = chars.length; i < len;) {
            char c = chars[i++];
            switch (c) {
                case 0:
                case '\n':
                case '\r':
                    break;
                case '\\':
                    c = chars[i++];
                    switch (c) {
                        case 'b':
                            buffer.append('\b');
                            break;
                        case 't':
                            buffer.append('\t');
                            break;
                        case 'n':
                            buffer.append('\n');
                            break;
                        case 'f':
                            buffer.append('\f');
                            break;
                        case 'r':
                            buffer.append('\r');
                            break;
                        case 'u':
                            buffer.append((char) Integer.parseInt(new String(new char[] {//
                                    chars[i++], chars[i++], chars[i++], chars[i++] //
                            }), 16));//
                            break;
                        case 'x':
                            buffer.append((char) Integer.parseInt(new String(new char[] {//
                                    chars[i++], chars[i++] //
                            }), 16));//
                            break;
                        default:
                            buffer.append(c);
                    }
                    break;
                default:
                    buffer.append(c);
            }
        }
        return buffer.toString();
    }
}
