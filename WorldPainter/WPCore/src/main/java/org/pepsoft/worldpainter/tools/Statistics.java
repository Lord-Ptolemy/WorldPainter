/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1;
import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_2;

/**
 *
 * @author pepijn
 */
public class Statistics {
    public static void main(String[] args) throws IOException {
        File levelDatFile = new File(args[0]);
        Level level = Level.load(levelDatFile);
        if ((level.getVersion() != SUPPORTED_VERSION_1) && (level.getVersion() != SUPPORTED_VERSION_2)) {
            throw new UnsupportedOperationException("Level format version " + level.getVersion() + " not supported");
        }
        int maxHeight = level.getMaxHeight();
        int maxY = maxHeight - 1;
        File worldDir = levelDatFile.getParentFile();
        File regionDir = new File(worldDir, "region");
        int version = level.getVersion();
        final Pattern regionFilePattern = (version == SUPPORTED_VERSION_1) ? Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr") : Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
        File[] regionFiles = regionDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return regionFilePattern.matcher(name).matches();
            }
        });
        int[][] blockTypeCounts = new int[maxHeight >> 4][4096];
        int[][] blockTypeTotals = new int[maxHeight >> 4][4096];
//        int totalBlockCount = 0, totalBlocksPerLevel = 0;
        System.out.println("Scanning " + worldDir);
        System.out.print('|');
        for (int i = 0; i < regionFiles.length - 2; i++) {
            System.out.print('-');
        }
        System.out.println('|');
        for (File file: regionFiles) {
            RegionFile regionFile = new RegionFile(file);
            try {
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        if (regionFile.containsChunk(x, z)) {
//                            totalBlocksPerLevel += 256;
                            
//                            System.out.println("Chunk " + x + ", " + z);
//                            System.out.print('.');
                            Tag tag;
                            NBTInputStream in = new NBTInputStream(regionFile.getChunkDataInputStream(x, z));
                            try {
                                tag = in.readTag();
                            } finally {
                                in.close();
                            }
                            Chunk chunk = (version == SUPPORTED_VERSION_1)
                                ? new ChunkImpl((CompoundTag) tag, maxHeight)
                                : new ChunkImpl2((CompoundTag) tag, maxHeight);

                            for (int xx = 0; xx < 16; xx++) {
                                for (int zz = 0; zz < 16; zz++) {
                                    for (int y = maxY; y >= 0; y--) {
                                        int blockType = chunk.getBlockType(xx, y, zz);
                                        int dataValue = chunk.getDataValue(xx, y, zz);
                                        blockTypeCounts[y >> 4][(blockType << 4) | dataValue]++;
                                        blockTypeTotals[y >> 4][(blockType << 4) | dataValue]++;
//                                        totalBlockCount++;
                                    }
                                }
                            }
                        }
                    }
                }
//                System.out.println();
            } finally {
                regionFile.close();
            }
            System.out.print('#');
        }
        System.out.println();
        
        System.out.println("\tGranite\tDiorite\tAndesite");
        for (int y = 0; y < maxHeight >> 4; y++) {
            int stoneLikeTotal = blockTypeTotals[y][index(Material.STONE)]
                               + blockTypeTotals[y][index(Material.GRANITE)]
                               + blockTypeTotals[y][index(Material.DIORITE)]
                               + blockTypeTotals[y][index(Material.ANDESITE)];
//            System.out.println("Total stonelike blocks: " + stoneLikeTotal);
            System.out.print(y + "\t");
            System.out.printf("%6.2f‰\t", ((float) blockTypeTotals[y][index(Material.GRANITE)] / stoneLikeTotal * 1000));
            System.out.printf("%6.2f‰\t", ((float) blockTypeTotals[y][index(Material.DIORITE)] / stoneLikeTotal * 1000));
            System.out.printf("%6.2f‰%n", ((float) blockTypeTotals[y][index(Material.ANDESITE)] / stoneLikeTotal * 1000));
        }
    }

    private static int index(Material material) {
        return (material.getBlockType() << 4) | material.getData();
    }
}