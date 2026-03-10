export function isFestivoItaliano(dateString: string): boolean {
    const date = new Date(`${dateString}T00:00:00`);

    if (Number.isNaN(date.getTime())) {
        return false;
    }

    if (date.getDay() === 0) {
        return true;
    }

    const month = date.getMonth() + 1;
    const day = date.getDate();

    const fixedFestivities = new Set([
        '1-1',
        '1-6',
        '4-25',
        '5-1',
        '6-2',
        '8-15',
        '11-1',
        '12-8',
        '12-25',
        '12-26',
    ]);

    if (fixedFestivities.has(`${month}-${day}`)) {
        return true;
    }

    const easterMonday = getEasterSunday(date.getFullYear());
    easterMonday.setDate(easterMonday.getDate() + 1);

    return (
        date.getFullYear() === easterMonday.getFullYear() &&
        date.getMonth() === easterMonday.getMonth() &&
        date.getDate() === easterMonday.getDate()
    );
}

function getEasterSunday(year: number): Date {
    const a = year % 19;
    const b = Math.floor(year / 100);
    const c = year % 100;
    const d = Math.floor(b / 4);
    const e = b % 4;
    const f = Math.floor((b + 8) / 25);
    const g = Math.floor((b - f + 1) / 3);
    const h = (19 * a + b - d - g + 15) % 30;
    const i = Math.floor(c / 4);
    const k = c % 4;
    const l = (32 + 2 * e + 2 * i - h - k) % 7;
    const m = Math.floor((a + 11 * h + 22 * l) / 451);
    const month = Math.floor((h + l - 7 * m + 114) / 31);
    const day = ((h + l - 7 * m + 114) % 31) + 1;

    return new Date(year, month - 1, day);
}