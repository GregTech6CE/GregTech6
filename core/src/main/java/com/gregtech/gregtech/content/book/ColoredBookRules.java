/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package com.gregtech.gregtech.content.book;
import java.util.List;
/** GT6 MultiItemBooks: eleven covers in two sizes, plus the six hidden manual/dictionary covers. */
public final class ColoredBookRules {
    private ColoredBookRules() {}
    public record Variant(String path,int originalId,boolean large,String color,int tint,String shelfTexture,boolean hidden,String emblem) {}
    public static final List<Variant> VARIANTS=List.of(
        new Variant("book_black",0,false,"black",0x202020,"book_colored",false,""),
        new Variant("book_white",1,false,"white",0xFFFFFF,"book_colored",false,""),
        new Variant("book_red",2,false,"red",0xFF0000,"book_colored",false,""),
        new Variant("book_green",3,false,"green",0x00FF00,"book_colored",false,""),
        new Variant("book_blue",4,false,"blue",0x0000FF,"book_colored",false,""),
        new Variant("book_cyan",5,false,"cyan",0x00FFFF,"book_colored",false,""),
        new Variant("book_magenta",6,false,"magenta",0xFF00FF,"book_colored",false,""),
        new Variant("book_yellow",7,false,"yellow",0xFFFF00,"book_colored",false,""),
        new Variant("book_brown",8,false,"brown",0x604000,"book_vanilla",false,""),
        new Variant("book_orange",9,false,"orange",0xFF8000,"book_colored",false,""),
        new Variant("book_purple",10,false,"purple",0x800080,"book_colored",false,""),
        new Variant("large_book_black",1000,true,"black",0x202020,"book_colored",false,""),
        new Variant("large_book_white",1001,true,"white",0xFFFFFF,"book_colored",false,""),
        new Variant("large_book_red",1002,true,"red",0xFF0000,"book_colored",false,""),
        new Variant("large_book_green",1003,true,"green",0x00FF00,"book_colored",false,""),
        new Variant("large_book_blue",1004,true,"blue",0x0000FF,"book_colored",false,""),
        new Variant("large_book_cyan",1005,true,"cyan",0x00FFFF,"book_colored",false,""),
        new Variant("large_book_magenta",1006,true,"magenta",0xFF00FF,"book_colored",false,""),
        new Variant("large_book_yellow",1007,true,"yellow",0xFFFF00,"book_colored",false,""),
        new Variant("large_book_brown",1008,true,"brown",0x604000,"book_vanilla",false,""),
        new Variant("large_book_orange",1009,true,"orange",0xFF8000,"book_colored",false,""),
        new Variant("large_book_purple",1010,true,"purple",0x800080,"book_colored",false,""),
        new Variant("book_bronze",32000,false,"",0xFFFFFF,"book_gt",true,"bronze"),
        new Variant("large_book_bronze",32001,true,"",0xFFFFFF,"book_gt",true,"bronze"),
        new Variant("book_dictionary",32002,false,"",0xFFFFFF,"book_matdict",true,"dictionary"),
        new Variant("large_book_dictionary",32003,true,"",0xFFFFFF,"book_matdict",true,"dictionary"),
        new Variant("book_radiation",32004,false,"",0xFFFFFF,"book_crafting",true,"radiation"),
        new Variant("large_book_radiation",32005,true,"",0xFFFFFF,"book_crafting",true,"radiation")
    );
    public static Variant byPath(String path){return VARIANTS.stream().filter(v->v.path().equals(path)).findFirst().orElse(null);}
    public static Variant byId(int id){return VARIANTS.stream().filter(v->v.originalId()==id).findFirst().orElseThrow();}
    public static int manualCover(String name,int pages){
        if(name.equals("Manual_Punch_Cards"))return 1;
        if(name.equals("Manual_Microwave"))return 2;
        if(name.equals("Manual_Portal_TF"))return 1005;
        if(name.equals("Manual_Enchantments"))return 10;
        if(name.startsWith("Manual_Hunting_"))return 3;
        if(name.equals("Manual_Reactors"))return 32005;
        return pages>50?32001:32000;
    }
    public static boolean isBook(String id){return id.startsWith("gregtech:")&&byPath(id.substring(9))!=null;}
}
