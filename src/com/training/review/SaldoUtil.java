package com.training.review;

import java.io.File;
import java.util.Scanner;

public class SaldoUtil {

    public String formatNamaNasabah(Nasabah nasabah) {
        return nasabah.getNama().toUpperCase();
    }

    public String bacaBarisPertama(String path) throws Exception {
        Scanner scanner = new Scanner(new File(path));
        String baris = scanner.nextLine();
        return baris;
    }

    public int hitungTotalSaldo(int[] saldoAkun) {
        int total = 0;
        for (int s : saldoAkun) {
            total = total + s;
        }
        return total;
    }

    public String buatQueryTransfer(String akunTujuan, long nominal) {
        return "UPDATE saldo SET nilai = nilai + " + nominal
             + " WHERE akun = '" + akunTujuan + "'";
    }

    public boolean isStatusAktif(String status) {
        return status == "AKTIF";
    }

    public double rataRataSaldo(long totalSaldo, int jumlahAkun) {
        if (jumlahAkun == 0) {
            return 0.0;
        }
        return (double) totalSaldo / jumlahAkun;
    }

    public double hitungBunga(double pokok, double rate, int tahun) {
        return pokok * rate * tahun;
    }
}
