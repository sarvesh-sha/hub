/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra.various;

import static org.junit.Assert.fail;

import java.io.InputStream;

import com.optio3.infra.IntelHexParser;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.Resources;
import org.junit.Test;

public class IntelHexParserTest extends Optio3Test
{
    @Test
    public void testParse() throws
                            Exception
    {
        compareImages("IntelHex/ex_boot_uart.production_v3_1.hex", "IntelHex/MCU8b_Optio_FDNY_76_Mar2020.X.production.unified_v301.hex");
        compareImages("IntelHex/ex_boot_uart.production_v2_2.hex", "IntelHex/MCU8b_Optio_FDNY_March2020.X.production.unified_V204_RETRO.hex");
        compareImages("IntelHex/ex_boot_uart.production_MTA_v2_1.hex", "IntelHex/MCU8b_MTA_North.V202.production.unified.hex");
        compareImages("IntelHex/ex_boot_uart.production_AMR_LTE_v1.1.hex", "IntelHex/MCU8b_AMR_LTE.v103.production.unified.hex");
        compareImages("IntelHex/ex_boot_uart.production_AMR_SP3X_v2.1.hex", "IntelHex/MCU8b_AMR_SP3X.v203.production.unified.hex");
        compareImages("IntelHex/ex_boot_uart.production_PSEG_v1.1.hex", "IntelHex/MCU8b_PSEG.x.production_v101.hex");
        compareImages("IntelHex/ex_boot_uart.production_PSEG_v1.1.hex", "IntelHex/MCU8b_PSEG.x.production_v102.hex");
        compareImages("IntelHex/ex_boot_uart.production_PEP_v1.1.hex", "IntelHex/MCU8b_PEPCO.x.production_v101.hex");
        compareImages("IntelHex/ex_boot_uart.production_PEP_v1.1.hex", "IntelHex/MCU8b_PEPCO.x.production_v102.hex");
        compareImages("IntelHex/ex_boot_uart.production_capmetro.hex", "IntelHex/MCU8b_capmetro.X.production.v101.hex");
        compareImages("IntelHex/ex_boot_uart.production_capmetro.hex", "IntelHex/MCU8b_capmetro.X.production.v102.hex");
    }

    private static void compareImages(String bootloaderResource,
                                      String unifiedResource) throws
                                                              Exception
    {
        System.out.println("##################################################################");

        IntelHexParser bootloader;
        IntelHexParser unified;

        try (InputStream stream = Resources.openResourceAsStream(IntelHexParserTest.class, bootloaderResource))
        {
            bootloader = new IntelHexParser(stream);
        }

        try (InputStream stream = Resources.openResourceAsStream(IntelHexParserTest.class, unifiedResource))
        {
            unified = new IntelHexParser(stream);
        }

//        for (IntelHexParser.Chunk chunk : bootloader.chunks)
//        {
//            System.out.printf("bootloader: [%08x - %08x]\n", chunk.rangeStart, chunk.rangeEnd);
//        }

        byte[] bootloaderBefore = new byte[4 * 65536];
        spread(bootloader, bootloaderBefore);

        bootloader.compact();

        byte[] bootloaderAfter = new byte[4 * 65536];
        spread(bootloader, bootloaderAfter);

        assertArrayEquals(bootloaderBefore, bootloaderAfter);

        for (IntelHexParser.Chunk chunk : bootloader.chunks)
        {
            System.out.printf("bootloader (compacted): [%08x - %08x]\n", chunk.rangeStart, chunk.rangeEnd);
        }

        System.out.println("######################");

//        for (IntelHexParser.Chunk chunk : unified.chunks)
//        {
//            System.out.printf("unified: [%08x - %08x]\n", chunk.rangeStart, chunk.rangeEnd);
//        }

        byte[] unifiedBefore = new byte[4 * 65536];
        spread(unified, unifiedBefore);

        unified.compact();

        byte[] unifiedAfter = new byte[4 * 65536];
        spread(unified, unifiedAfter);

        assertArrayEquals(unifiedBefore, unifiedAfter);

        for (IntelHexParser.Chunk chunk : unified.chunks)
        {
            System.out.printf("unified (compacted): [%08x - %08x]\n", chunk.rangeStart, chunk.rangeEnd);
        }

        //--//

        for (IntelHexParser.Chunk chunk : bootloader.chunks)
        {
            for (int i = 0; i < chunk.data.length; i++)
            {
                int address = (int) (i + chunk.rangeStart);

                if (bootloaderAfter[address] != unifiedAfter[address])
                {
                    fail(String.format("Unified overwrites bootloader at %08x!", address));
                }
            }
        }

        System.out.printf("Bootloader (%s) and Unified (%s) images match!!\n\n", bootloaderResource, bootloaderResource);
    }

    private static void spread(IntelHexParser parser,
                               byte[] memoryMap)
    {
        for (IntelHexParser.Chunk chunk : parser.chunks)
        {
            System.arraycopy(chunk.data, 0, memoryMap, (int) chunk.rangeStart, chunk.data.length);
        }
    }
}
