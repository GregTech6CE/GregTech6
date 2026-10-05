package com.gregtech.gregtech.core;

import com.gregtech.gregtech.content.logistics.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Source samples for forks, claimed receivers and lines beyond the old arbitrary scan cap. */
final class LongDistanceSourceContracts {
    private static int checks;
    static int verify() {
        checks=0;
        var living=new AtomicBoolean(true);
        var a=new LongDistanceLink<Integer,String>(0,"A",living::get);
        var b=new LongDistanceLink<Integer,String>(10,"B",()->true);
        var receiver=new LongDistanceLink<Integer,String>(20,"R",()->true);
        require(a.beginScan()&&b.beginScan(),"sources scan");
        a.bind(receiver);b.bind(receiver);
        require(a.claim(),"first sender claims receiver");
        require(!b.claim(),"another live sender cannot steal it");
        require(receiver.receiving()&&!receiver.beginScan(),"receiving role blocks soft-hammer scan");
        require("A".equals(receiver.senderOwner()),"receiver reports sender");
        living.set(false);
        require(b.claim()&&"B".equals(receiver.senderOwner()),"dead sender releases ownership");
        b.reset();a.restore(20);living.set(true);a.bind(receiver);
        require(a.claim(),"sender with reset target no longer owns receiver");
        require(a.targetPosition()==20&&"R".equals(a.targetOwner()),"remembered target and owner");
        a.reset();require(!a.known(),"coordinate or facing change clears target");

        var network=new LongDistanceNetwork.World<Integer,Integer>() {
            public String identity(Integer p){return p>0&&p<=5000?"Tin":null;}
            public Iterable<Integer> neighbors(Integer p){return List.of(p-1,p+1);}
            public Integer endpoint(Integer p){return p==5001?p:null;}
            public Integer front(Integer endpoint){return endpoint-1;}
        };
        var longRoute=LongDistanceNetwork.scan(0,1,"Tin",network);
        require(longRoute!=null&&longRoute.distance()==5000&&longRoute.lines().size()==5000,"5000-line source scan is not cut at 4096");
        var fork=new LongDistanceNetwork.World<Integer,Integer>() {
            public String identity(Integer p){return p>=1&&p<=3?"Tin":null;}
            public Iterable<Integer> neighbors(Integer p){return p==1?List.of(2,-5):List.of(p-1,p+1);}
            public Integer endpoint(Integer p){return p==-5||p==4?p:null;}
            public Integer front(Integer endpoint){return endpoint==-5?1:3;}
        };
        var forkRoute=LongDistanceNetwork.scan(0,1,"Tin",fork);
        require(forkRoute!=null&&forkRoute.receiver()==-5&&forkRoute.distance()==1,"fork chooses first reachable receiver instead of rejecting network");
        require(LongDistanceNetwork.scan(0,1,"Lead",fork)==null,"same voltage is not same material");
        require(LongDistanceNetwork.placement(64.99f,2)==2,"below pitch threshold stays horizontal");
        require(LongDistanceNetwork.placement(65,2)==1,"looking down places front up");
        require(LongDistanceNetwork.placement(-65,2)==0,"looking up places front down");
        require(LongDistanceNetwork.placement(-64.99f,2)==2,"negative threshold is inclusive only at 65");
        require(LongDistanceNetwork.activity(0,false,false)==0,"idle");
        require(LongDistanceNetwork.activity(-1,false,false)==1,"64 active ticks");
        require(LongDistanceNetwork.activity(1,false,false)==2,"intermittent");
        require(LongDistanceNetwork.activity(-1,true,true)==0,"stopped");
        require(LongDistanceNetwork.activity(0,false,true)==3,"remembered unloaded target");
        require(LongDistanceNetwork.nextHistory(-1,false,true,false)==-1,"stopped display preserves 64 active source ticks");
        require(LongDistanceNetwork.nextHistory(9,false,false,true)==9,"unavailable target preserves recent activity");
        return checks;
    }
    private static void require(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
}
