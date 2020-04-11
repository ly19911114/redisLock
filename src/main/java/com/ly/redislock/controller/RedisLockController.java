package com.ly.redislock.controller;

import com.ly.redislock.service.RedisService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @author LiuYang
 * @date 2020/4/11
 */
@RestController
public class RedisLockController {

    @Resource(name = "redisService")
    private RedisService redisService;

    //没有连接db讲车票设置为类变量作为共享资源，便于任务竞争进行DEMO展示
    private int trainNum = 500;

    /**
     * 无db，模拟补充车票
     *
     * @param num
     */
    @RequestMapping(value = "/addTrainNum")
    public String addTrainNum(@RequestParam(defaultValue = "500") int num) {
        trainNum = trainNum + num;
        return "系统已补票，目前车票库存:" + trainNum;
    }

    /**
     * 无锁买票
     */
    @RequestMapping(value = "/noLock")
    public void noLock() {

        while (true) {
            if (trainNum > 0) {
                try {
                    Thread.sleep(20);
                    System.out.println(Thread.currentThread().getName() + "购买了车票，车票号：" + trainNum--);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                }
            } else {
                System.out.println("车票售完");
                break;
            }
        }
    }


    /**
     * redis锁票购买
     */
    @RequestMapping(value = "/redisLock")
    public void redisLock() {
        String uuid = UUID.randomUUID().toString();
        while (true) {
            redisService.lock("train_test", uuid);
            if (trainNum > 0) {
                try {
                    Thread.sleep(20);
                    System.out.println(Thread.currentThread().getName() + "购买了车票，车票号是: " + trainNum-- + ",uuid:" + uuid);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    redisService.unlock("train_test", uuid);
                }
            } else {
                System.out.println("车票售完");
                redisService.unlock("train_test", uuid);
                break;
            }
        }
    }

    /**
     * 本地多线程模拟
     */
    @RequestMapping(value = "/redisLockTest")
    public void redisLockTest() {

        Train train = new Train(40);
        Thread thread1 = new Thread(train, "小明");
        Thread thread2 = new Thread(train, "小王");
        Thread thread3 = new Thread(train, "小李");
        thread1.start();
        thread2.start();
        thread3.start();
    }


    public class Train implements Runnable {

        private int num;
        private ThreadLocal<String> localUUID = new ThreadLocal<>();

        public Train(int num) {
            this.num = num;
        }


        @Override
        public void run() {
         localUUID.set(UUID.randomUUID().toString());
         while (true){
             redisService.lock("train_test",localUUID.get());
             if (num>0){
                 try {
                     Thread.sleep(100);
                     System.out.println(Thread.currentThread().getName() + "购买了车票，车票号：" + num-- + ",uuid:" + localUUID.get());
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }finally {
                     redisService.unlock("train_test",localUUID.get());
                 }
             }else {
                 redisService.unlock("train_test",localUUID.get());
                 System.out.println("车票售完");
                 break;
             }
         }
        }
    }


}
