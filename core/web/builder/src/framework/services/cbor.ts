import {Lookup} from "framework/services/utils.service";

export class ArrayBufferReader
{
    private m_view: DataView;
    private m_byteView: Uint8Array;
    private m_offset = 0;

    constructor(private m_buffer: ArrayBuffer)
    {
        this.m_view     = new DataView(this.m_buffer, 0, this.m_buffer.byteLength);
        this.m_byteView = new Uint8Array(this.m_buffer);
    }

    public get isEof(): boolean
    {
        return this.m_offset == this.m_view.byteLength;
    }

    public get offset(): number
    {
        return this.m_offset;
    }

    public peekUint8(): number
    {
        return this.isEof ? undefined : this.m_view.getUint8(this.m_offset);
    }

    public getFloat32(): number
    {
        let offset = this.m_offset;
        this.m_offset += 4;
        return this.m_view.getFloat32(offset, false);
    }

    public getFloat64(): number
    {
        let offset = this.m_offset;
        this.m_offset += 8;
        return this.m_view.getFloat64(offset, false);
    }

    public getInt8(): number
    {
        let offset = this.m_offset;
        this.m_offset += 1;
        return this.m_view.getInt8(offset);
    }

    public getInt16(): number
    {
        let offset = this.m_offset;
        this.m_offset += 2;
        return this.m_view.getInt16(offset, false);
    }

    public getInt32(): number
    {
        let offset = this.m_offset;
        this.m_offset += 4;
        return this.m_view.getInt32(offset, false);
    }

    public getUint8(): number
    {
        let offset = this.m_offset;
        this.m_offset += 1;
        return this.m_view.getUint8(offset);
    }

    public getUint16(): number
    {
        let offset = this.m_offset;
        this.m_offset += 2;
        return this.m_view.getUint16(offset, false);
    }

    public getUint32(): number
    {
        let offset = this.m_offset;
        this.m_offset += 4;
        return this.m_view.getUint32(offset, false);
    }

    public getBytes(length: number): Uint8Array
    {
        let offset = this.m_offset;
        this.m_offset += length;
        return new Uint8Array(this.m_buffer.slice(offset, this.m_offset));
    }
}

export class ArrayBufferWriter
{
    private m_buffer: ArrayBuffer;
    private m_view: DataView;
    private m_byteView: Uint8Array;
    private m_offset    = 0;
    private m_bufferLen = 0;

    constructor(buffer?: ArrayBuffer)
    {
        if (!buffer) buffer = new ArrayBuffer(1024);

        this.m_buffer    = buffer;
        this.m_bufferLen = buffer.byteLength;
        this.m_view      = new DataView(buffer, 0, buffer.byteLength);
        this.m_byteView  = new Uint8Array(buffer);

    }

    public toArray(): ArrayBuffer
    {
        return this.slice(0, this.m_offset);
    }

    public slice(start: number,
                 end?: number): ArrayBuffer
    {
        return this.m_buffer.slice(start, end);
    }

    public setFloat32(value: number): void
    {
        let offset = this.resize(4);
        this.m_view.setFloat32(offset, value, false);
    }

    public setFloat64(value: number): void
    {
        let offset = this.resize(8);
        this.m_view.setFloat64(offset, value, false);
    }

    public setInt8(value: number): void
    {
        let offset = this.resize(1);
        this.m_view.setInt8(offset, value);
    }

    public setInt16(value: number): void
    {
        let offset = this.resize(2);
        this.m_view.setInt16(offset, value, false);
    }

    public setInt32(value: number): void
    {
        let offset = this.resize(4);
        this.m_view.setInt32(offset, value, false);
    }

    public setUint8(value: number): void
    {
        let offset = this.resize(1);
        this.m_view.setUint8(offset, value);
    }

    public setUint16(value: number): void
    {
        let offset = this.resize(2);
        this.m_view.setUint16(offset, value, false);
    }

    public setUint32(value: number): void
    {
        let offset = this.resize(4);
        this.m_view.setUint32(offset, value, false);
    }

    public setBytes(value: Uint8Array): void
    {
        let offset = this.resize(value.byteLength);
        this.m_byteView.set(value, offset);
    }

    private resize(size: number): number
    {
        let offset = this.m_offset + size;

        if (offset >= this.m_bufferLen)
        {
            let buffer   = new ArrayBuffer(offset * 2);
            let byteView = new Uint8Array(buffer);

            byteView.set(this.m_byteView);

            this.m_buffer    = buffer;
            this.m_byteView  = byteView;
            this.m_view      = new DataView(buffer);
            this.m_bufferLen = buffer.byteLength;
        }

        return offset;
    }
}

const POW_2_24 = 5.960464477539063e-8;
const POW_2_32 = 4294967296;
const POW_2_53 = 9007199254740992;

export class CborDecoderError extends Error
{
    constructor(what: string)
    {
        super(what);
    }
}

export class CborDecoder
{
    private readonly m_tempArrayBuffer = new ArrayBuffer(8);
    private readonly m_tempDataView    = new DataView(this.m_tempArrayBuffer);

    constructor(private reader: ArrayBufferReader)
    {
    }

    decode(): any
    {
        let res = this.decodeItem();
        if (!this.reader.isEof)
        {
            throw new CborDecoderError(`Decoding ended early at ${this.reader.offset}`);
        }

        return res;
    }

    public taggedValue(tagId: number,
                        value: any): any
    {
        /*
            Tag	Data Item	Semantics
            0	text string	Standard date/time string; see Section 3.4.1
            1	integer or float	Epoch-based date/time; see Section 3.4.2
            2	byte string	Unsigned bignum; see Section 3.4.3
            3	byte string	Negative bignum; see Section 3.4.3
            4	array	Decimal fraction; see Section 3.4.4
            5	array	Bigfloat; see Section 3.4.4
            21	(any)	Expected conversion to base64url encoding; see Section 3.4.5.2
            22	(any)	Expected conversion to base64 encoding; see Section 3.4.5.2
            23	(any)	Expected conversion to base16 encoding; see Section 3.4.5.2
            24	byte string	Encoded CBOR data item; see Section 3.4.5.1
            32	text string	URI; see Section 3.4.5.3
            33	text string	base64url; see Section 3.4.5.3
            34	text string	base64; see Section 3.4.5.3
            36	text string	MIME message; see Section 3.4.5.3
            55799	(any)	Self-described CBOR; see Section 3.4.6
         */
        switch (tagId)
        {
            case 4:
            {
                // A decimal fraction or a bigfloat is represented as a tagged array that contains exactly two integer numbers: an exponent e and a mantissa m.
                // Decimal fractions (tag number 4) use base-10 exponents; the value of a decimal fraction data item is m*(10^e).
                let array = <number[]> value;
                return array[1] * Math.pow(10, array[0]);
            }

            case 5:
            {
                // A decimal fraction or a bigfloat is represented as a tagged array that contains exactly two integer numbers: an exponent e and a mantissa m.
                // Bigfloats (tag number 5) use base-2 exponents; the value of a bigfloat data item is m*(2^e).
                let array = <number[]> value;
                return array[1] * Math.pow(2, array[0]);
            }
        }

        throw new CborDecoderError(`Unrecognized tag ${tagId} at ${this.reader.offset}`);
    }

    public simpleValue(value: number): any
    {
        throw new CborDecoderError(`Unrecognized simple value '${value}' at ${this.reader.offset}`);
    }

    //--//

    private decodeItem(): any
    {
        let initialByte           = this.readUint8();
        let majorType             = initialByte >> 5;
        let additionalInformation = initialByte & 0x1f;

        if (majorType === 7)
        {
            switch (additionalInformation)
            {
                case 25:
                    return this.readFloat16();
                case 26:
                    return this.readFloat32();
                case 27:
                    return this.readFloat64();
            }
        }

        let length = this.readLength(additionalInformation);
        if (length < 0 && (majorType < 2 || 6 < majorType))
        {
            throw new CborDecoderError(`Invalid length at ${this.reader.offset}`);
        }

        switch (majorType)
        {
            case 0:
                return length;

            case 1:
                return -1 - length;

            case 2:
                if (length < 0)
                {
                    let elements        = [];
                    let fullArrayLength = 0;
                    while ((length = this.readIndefiniteStringLength(majorType)) >= 0)
                    {
                        fullArrayLength += length;
                        elements.push(this.readArrayBuffer(length));
                    }
                    let fullArray       = new Uint8Array(fullArrayLength);
                    let fullArrayOffset = 0;
                    for (let i = 0; i < elements.length; ++i)
                    {
                        fullArray.set(elements[i], fullArrayOffset);
                        fullArrayOffset += elements[i].length;
                    }
                    return fullArray;
                }

                return this.readArrayBuffer(length);

            case 3:
            {
                let utf16data: number[] = [];
                if (length < 0)
                {
                    while ((length = this.readIndefiniteStringLength(majorType)) >= 0)
                    {
                        this.appendUtf16Data(utf16data, length);
                    }
                }
                else
                {
                    this.appendUtf16Data(utf16data, length);
                }

                return String.fromCharCode(...utf16data);
            }

            case 4:
                if (length < 0)
                {
                    let retArray = [];
                    while (!this.readBreak())
                    {
                        retArray.push(this.decodeItem());
                    }
                    return retArray;
                }
                else
                {
                    let retArray = new Array(length);
                    for (let i = 0; i < length; ++i)
                    {
                        retArray[i] = this.decodeItem();
                    }
                    return retArray;
                }

            case 5:
            {
                let retObject = {};
                let indexer   = retObject as Lookup<any>;
                for (let i = 0; i < length || (length < 0 && !this.readBreak()); ++i)
                {
                    let key      = this.decodeItem();
                    indexer[key] = this.decodeItem();
                }
                return retObject;
            }

            case 6:
                return this.taggedValue(length, this.decodeItem());

            case 7:
                switch (length)
                {
                    case 20:
                        return false;

                    case 21:
                        return true;

                    case 22:
                        return null;

                    case 23:
                        return undefined;

                    default:
                        return this.simpleValue(length);
                }
        }
    }

    private readArrayBuffer(length: number): Uint8Array
    {
        return this.reader.getBytes(length);
    }

    private readFloat16(): number
    {
        var value = this.readUint16();

        var sign     = value & 0x8000;
        var exponent = value & 0x7c00;
        var fraction = value & 0x03ff;

        if (exponent === 0x7c00)
        {
            exponent = 0xff << 10;
        }
        else if (exponent !== 0)
        {
            exponent += (127 - 15) << 10;
        }
        else if (fraction !== 0)
        {
            return (sign ? -1 : 1) * fraction * POW_2_24;
        }

        this.m_tempDataView.setUint32(0, sign << 16 | exponent << 13 | fraction << 13);
        return this.m_tempDataView.getFloat32(0);
    }

    private readFloat32(): number
    {
        return this.reader.getFloat32();
    }

    private readFloat64(): number
    {
        return this.reader.getFloat64();
    }

    private readUint8()
    {
        return this.reader.getUint8();
    }

    private readUint16()
    {
        return this.reader.getUint16();
    }

    private readUint32()
    {
        return this.reader.getUint32();
    }

    private readUint64()
    {
        let high = this.readUint32();
        let low  = this.readUint32();
        return high * POW_2_32 + low;
    }

    private readBreak(): boolean
    {
        if (this.reader.peekUint8() === 0xFF)
        {
            this.reader.getUint8();
            return true;
        }

        return false;
    }

    private readLength(val: number): number
    {
        if (val < 24)
        {
            return val;
        }

        switch (val)
        {
            case  24:
                return this.readUint8();

            case 25:
                return this.readUint16();

            case 26:
                return this.readUint32();

            case 27:
                return this.readUint64();

            case 31:
                return -1;

            default:
                throw new CborDecoderError(`Invalid length encoding at ${this.reader.offset}`);
        }
    }

    private readIndefiniteStringLength(majorType: number)
    {
        let initialByte = this.readUint8();
        if (initialByte === 0xff)
        {
            return -1;
        }

        let length = this.readLength(initialByte & 0x1f);
        if (length < 0 || (initialByte >> 5) !== majorType)
        {
            throw new CborDecoderError(`Invalid indefinite length element at ${this.reader.offset}`);
        }
        return length;
    }

    private appendUtf16Data(utf16data: number[],
                            length: number)
    {
        for (let i = 0; i < length; ++i)
        {
            let value = this.readUint8();
            if (value & 0x80)
            {
                if (value < 0xe0)
                {
                    value = (value & 0x1f) << 6 | (this.readUint8() & 0x3f);
                    length -= 1;
                }
                else if (value < 0xf0)
                {
                    value = (value & 0x0f) << 12 | (this.readUint8() & 0x3f) << 6 | (this.readUint8() & 0x3f);
                    length -= 2;
                }
                else
                {
                    value = (value & 0x0f) << 18 | (this.readUint8() & 0x3f) << 12 | (this.readUint8() & 0x3f) << 6 | (this.readUint8() & 0x3f);
                    length -= 3;
                }
            }

            if (value < 0x10000)
            {
                utf16data.push(value);
            }
            else
            {
                value -= 0x10000;
                utf16data.push(0xd800 | (value >> 10));
                utf16data.push(0xdc00 | (value & 0x3ff));
            }
        }
    }
}
