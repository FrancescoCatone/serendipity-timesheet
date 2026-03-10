package com.serendipity.backend.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;

@Service
public class CalendarioFestivitaService {

    public boolean isFestivo(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }

        MonthDay md = MonthDay.from(date);

        if (
                md.equals(MonthDay.of(1, 1)) ||
                        md.equals(MonthDay.of(1, 6)) ||
                        md.equals(MonthDay.of(4, 25)) ||
                        md.equals(MonthDay.of(5, 1)) ||
                        md.equals(MonthDay.of(6, 2)) ||
                        md.equals(MonthDay.of(8, 15)) ||
                        md.equals(MonthDay.of(11, 1)) ||
                        md.equals(MonthDay.of(12, 8)) ||
                        md.equals(MonthDay.of(12, 25)) ||
                        md.equals(MonthDay.of(12, 26))
        ) {
            return true;
        }

        LocalDate pasquetta = easterSunday(date.getYear()).plusDays(1);
        return date.equals(pasquetta);
    }

    private LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(year, month, day);
    }
}
