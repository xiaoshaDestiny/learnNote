package com.learn.boot.controller;


import com.google.common.collect.Lists;
import com.learn.boot.dto.Baggage;
import com.learn.boot.dto.Box;
import com.learn.boot.dto.Tourist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xu.rb
 * @description: TODO
 * @since 2021-05-15 01:24
 */
@RestController
public class CheckController {

    @Autowired
    private CheckService checkService;

    private static List<Tourist> touristLists;

    private static List<Baggage> baggageList;

    static {
        Box blackBox = new Box("black", 100);
        Box redBox = new Box("red", 10);

        Baggage blackBaggage = new Baggage("black", redBox, "001");
        Baggage redBaggage = new Baggage("red", blackBox, "002");
        Baggage blueBaggage = new Baggage("black", blackBox, "003");

        //tom 拉着有一个红色的行李箱(002号)，里面装有黑色的盒子,手里一个红色的盒子
        Tourist tom = new Tourist("tom", 1, redBox, redBaggage);

        //jerry 手里拿着一个黑色的盒子
        Tourist jerry = new Tourist("jerry", 1, blackBox, blueBaggage);

        //donald 手里拿着一个红色的盒子，拉着一个黑色的行李箱(001号)，行李箱里面有红色的盒子
        Tourist donald = new Tourist("donald", 1, redBox, blackBaggage);

        touristLists = Lists.newArrayList(tom, jerry, donald);
        baggageList = Lists.newArrayList(blackBaggage, redBaggage, blueBaggage);
    }

    @RequestMapping(value = "/tour/check")
    public String checkTour(@RequestParam("name") String name){
        Tourist tourist = touristLists.stream().filter(tour -> tour.getName().equalsIgnoreCase(name)).collect(Collectors.toList()).get(0);
        return checkService.tourService(tourist);
    }


    @RequestMapping(value = "/pak/check")
    public String pakTour(@RequestParam("no") String no){
        Map<String, Tourist> packToTour = touristLists.stream().collect(Collectors.toMap(tour -> tour.getPack().getNo(), Function.identity()));
        Baggage baggage = baggageList.stream().filter(bag -> bag.getNo().equalsIgnoreCase(no)).collect(Collectors.toList()).get(0);
        return checkService.baggageService(baggage, packToTour.get(no));
    }

}
