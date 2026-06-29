package net.datatecsolution.admintools.core;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * US-040 — Conversor de número a letras (Lempiras), PORT EXACTO del Swing
 * {@code net.datatecsolution.admin_tools.modelo.NumberToLetterConverter} para
 * que el "total en letras" del ticket fiscal salga idéntico al del sistema de
 * escritorio. Se usa al crear la factura para poblar
 * {@code encabezado_factura.total_letras} (antes "NA"). Rango válido [0, 999'999.999].
 *
 * Mantener en sincronía con el Swing si éste cambia el formato.
 */
public class NumberToLetterConverter {

    private static final String[] UNIDADES = {"", "UN ", "DOS ", "TRES ",
            "CUATRO ", "CINCO ", "SEIS ", "SIETE ", "OCHO ", "NUEVE ", "DIEZ ",
            "ONCE ", "DOCE ", "TRECE ", "CATORCE ", "QUINCE ", "DIECISEIS",
            "DIECISIETE", "DIECIOCHO", "DIECINUEVE", "VEINTE"};

    private static final String[] DECENAS = {"VEINTI", "TREINTA ", "CUARENTA ",
            "CINCUENTA ", "SESENTA ", "SETENTA ", "OCHENTA ", "NOVENTA ",
            "CIEN "};

    private static final String[] CENTENAS = {"CIENTO ", "DOSCIENTOS ",
            "TRESCIENTOS ", "CUATROCIENTOS ", "QUINIENTOS ", "SEISCIENTOS ",
            "SETECIENTOS ", "OCHOCIENTOS ", "NOVECIENTOS "};

    public static String convertNumberToLetter(String number) throws NumberFormatException {
        return convertNumberToLetter(Double.parseDouble(number));
    }

    public static String convertNumberToLetter(double doubleNumber) throws NumberFormatException {

        StringBuilder converted = new StringBuilder();

        // 2 decimales FIJOS con separador '.' (Locale.US) para que los centavos
        // queden siempre en 2 dígitos.
        DecimalFormat format = new DecimalFormat("0.00",
                DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        String formatedDouble = format.format(doubleNumber); // siempre "NNNN.DD"

        if (doubleNumber > 999999999)
            throw new NumberFormatException(
                    "El numero es mayor de 999'999.999, no es posible convertirlo");

        if (doubleNumber < 0)
            throw new NumberFormatException("El numero debe ser positivo");

        String[] splitNumber = formatedDouble.replace('.', '#').split("#");

        // Trío de millones
        int millon = Integer.parseInt(String.valueOf(getDigitAt(splitNumber[0], 8))
                + getDigitAt(splitNumber[0], 7)
                + getDigitAt(splitNumber[0], 6));
        if (millon == 1)
            converted.append("UN MILLON ");
        else if (millon > 1)
            converted.append(convertNumber(String.valueOf(millon)) + "MILLONES ");

        // Trío de miles
        int miles = Integer.parseInt(String.valueOf(getDigitAt(splitNumber[0], 5))
                + getDigitAt(splitNumber[0], 4)
                + getDigitAt(splitNumber[0], 3));
        if (miles == 1)
            converted.append("MIL ");
        else if (miles > 1)
            converted.append(convertNumber(String.valueOf(miles)) + "MIL ");

        // Último trío de unidades
        int cientos = Integer.parseInt(String.valueOf(getDigitAt(splitNumber[0], 2))
                + getDigitAt(splitNumber[0], 1)
                + getDigitAt(splitNumber[0], 0));
        if (cientos == 1)
            converted.append("UN");

        if (millon + miles + cientos == 0)
            converted.append("CERO");
        if (cientos > 1)
            converted.append(convertNumber(String.valueOf(cientos)));

        converted.append(" LEMPIRAS");

        // Centavos
        int centavos = Integer.parseInt(String.valueOf(getDigitAt(splitNumber[1], 2))
                + getDigitAt(splitNumber[1], 1)
                + getDigitAt(splitNumber[1], 0));
        if (centavos == 1)
            converted.append(" CON UN CENTAVO");
        else if (centavos > 1)
            converted.append(" CON " + convertNumber(String.valueOf(centavos)) + "CENTAVOS");

        return converted.toString().trim().replaceAll("\\s+", " ");
    }

    private static String convertNumber(String number) {

        if (number.length() > 3)
            throw new NumberFormatException("La longitud maxima debe ser 3 digitos");

        if (number.equals("100")) {
            return "CIEN";
        }

        StringBuilder output = new StringBuilder();
        if (getDigitAt(number, 2) != 0)
            output.append(CENTENAS[getDigitAt(number, 2) - 1]);

        int k = Integer.parseInt(String.valueOf(getDigitAt(number, 1)) + getDigitAt(number, 0));

        if (k <= 20)
            output.append(UNIDADES[k]);
        else if (k > 30 && getDigitAt(number, 0) != 0)
            output.append(DECENAS[getDigitAt(number, 1) - 2] + "Y " + UNIDADES[getDigitAt(number, 0)]);
        else
            output.append(DECENAS[getDigitAt(number, 1) - 2] + UNIDADES[getDigitAt(number, 0)]);

        return output.toString();
    }

    private static int getDigitAt(String origin, int position) {
        if (origin.length() > position && position >= 0)
            return origin.charAt(origin.length() - position - 1) - 48;
        return 0;
    }
}
