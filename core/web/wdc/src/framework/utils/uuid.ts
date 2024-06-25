export function uuid()
{
    return uuidGen();
}

// A very compact spec compliant uuid generator which is sufficient for our purposes
// https://gist.github.com/jed/982883
function uuidGen(a?: any)
{
    if (a)
    {
        // Return random number from 0 to 15 unless a is 8 in which case return
        // a random number from 8 to 11. Convert to hex
        return (a ^ Math.random() * 16 >> a / 4).toString(16);
    }

    return "00000000-0000-4000-8000-000000000000".replace(/[08]/g, uuidGen);
}
