package com.wp.demo.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.wp.demo.bean.Admin;
import com.wp.demo.bean.Commodity;
import com.wp.demo.bean.CommodityType;
import com.wp.demo.bean.Customer;
import com.wp.demo.service.ProductAndUserService;
import com.wp.demo.service.ProductService;
import com.wp.demo.utils.IPTimeStamp;
import com.wp.demo.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Administrator on 2019/1/2.
 */
@Controller
public class ShoppingController {

    @Autowired
    ProductService productService;

    @Autowired
    ProductAndUserService productAndUserService;

    //用于生成上传的商品照片名
    IPTimeStamp ipTimeStamp = new IPTimeStamp("192.168.1.1");

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    /**
     * 从属性域中取得所有已购买的商品信息
     * @param session
     * @return
     */
    public HashMap<Integer,Integer> getAllpid(HttpSession session){

        HashMap<Integer,Integer> allpid = (HashMap<Integer,Integer>)session.getAttribute("allpid");

        return allpid;
    }

    /**
     * 从属性域中取得admin对象
     * @param session
     * @return
     */
    public Admin getAdminFromSession(HttpSession session){
        Admin admin = (Admin) session.getAttribute("adminLoginUser");
        return admin;
    }

    //@ResponseBody
    @GetMapping("/shopping/goshopping")
    public String shoppingFirstPage(Model model) throws Exception {

        List<Commodity> all = productService.findAll();
        model.addAttribute("all",all);

        return "shopping/myshopping";
    }

    /**
     * 处理在搜索框中按关键字搜索商品
     * @param key
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/usr/ajax/seachCommodity")
    public String seachCommodity(@RequestParam(value = "key") String key){
        Page<Commodity> commodities = productService.seachCommodity(key);
        if(commodities != null && commodities.size() != 0 ){
            return "true";
        }

        return "false";
    }

    /**
     * 搜素到商品后进行跳转
     * @return
     */
    @GetMapping(value = "/usr/ajax/seachCommodity/{key}")
    public String seachCommodityJump(@PathVariable(value = "key") String key,
                                     @RequestParam(defaultValue = "1") int pageNo,
                                     @RequestParam(defaultValue = "8") int pageSize,
                                     Model model){

        PageHelper.startPage(pageNo, pageSize);
        Page<Commodity> commodities = productService.seachCommodity(key);
        PageInfo<Commodity> page = new PageInfo<>(commodities,5);
        model.addAttribute("pageInfo",page);

        return "shopping/myshopping";
    }

    //分页显示商品
    @GetMapping("/shopping/goshoppingbypage")
    public String getCommoditiesByPage(@RequestParam(defaultValue = "1") int pageNo,
                                       @RequestParam(defaultValue = "8") int pageSize,
                                       Model model,
                                       HttpSession session) throws Exception {
        PageHelper.startPage(pageNo, pageSize,true);
        Page<Commodity> all = productService.findAll();

        //使用pageInfo包装查询后的结果，只需要将pageInfo交给页面就行了
        //封装了详细的分页信息，包括我们查询出来的数据,传入连续显示的页数eg:5  表示连续显示5页
        PageInfo<Commodity> page = new PageInfo<>(all,5);
        model.addAttribute("pageInfo",page);
        if(session.getAttribute("adminLoginUser") != null){
            return "/adminPage/myshopping";
        }
        return "shopping/myshopping";
    }

    /**
     * 处理添加到购物车的ajax请求
     * @param pid
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/usr/ajax/addToShoppingCar")
    public String addToShoppingCar(@RequestParam(value = "pid") Integer pid, HttpSession session){

        Map<Integer,Integer> cars = getAllpid(session);
        if(cars == null){
            cars = new HashMap<>();                     //如果是第一次添加到购物车
        }
        if(cars.get(pid) == null){                     //若购物车中没有改商品,则向购物车中添加该商品
            cars.put(pid,1);
        }else {
            int val = cars.get(pid);
            val++;
            cars.put(pid,val);
        }
        session.setAttribute("allpid",cars);        //将所有购买的商品的pid保存到该用户对应的session域中

        return "true";
    }

    /**
     * 购物车界面，该界面可以查看购买了哪些商品，每件商品的数量
     * @param session
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/usr/toOrderCar",method = RequestMethod.GET)
    public String toOrderCar(HttpSession session,Model model) throws Exception {

        HashMap<Integer,Integer> allpid = getAllpid(session);
        Map<Integer,Commodity> commodities = new HashMap();

        if(allpid != null){
            Set<Integer> keys = allpid.keySet();
            Iterator<Integer> iterator = keys.iterator();
            int sumCount = 0;           //总共购买的商品数量
            double sumPrice = 0;        //总共价钱
            int pid = 0;
            int commodityCount = 0;     //每件商品的数量
            Map<Integer,Integer> cars = new HashMap<>();

            while (iterator.hasNext()){
                pid = iterator.next();
                commodityCount = allpid.get(pid);
                //根据加入购物车的商品的pid从数据库查询出商品
                Commodity commodity = productService.findById(pid);

                //计算商品的总价钱
                sumPrice = sumPrice + commodity.getPrice() * commodityCount;

                //将商品的pid和对应的购买数量作为键值对，方便结算页面取得商品数量
                cars.put(commodity.getPid(),commodityCount);
                //以购买每件商品的pid作为key，商品作为value
                commodities.put(commodity.getPid(),commodity);
                sumCount++;
            }

            model.addAttribute("cars",cars);
            model.addAttribute("allCommotity",commodities);
            model.addAttribute("sumCount",sumCount);
            model.addAttribute("sumPrice",sumPrice);
        }
        //如果是管理员登录
        if (session.getAttribute("adminLoginUser") != null){
            return "adminPage/myShoppingCar";
        }
        return "shopping/myShoppingCar";
    }

    /**
     * 由购物车中勾选好商品后
     * @param checkboxpid
     * @param model
     * @return
     */
    @RequestMapping(value = "/usr/ajax/toMyOrder/{checkboxpid}/{commodityCount}" ,method = RequestMethod.GET)
    public String myOder(@PathVariable("checkboxpid") String checkboxpid,@PathVariable("commodityCount")
            String commodityCount, Model model,HttpSession session) throws Exception {

        buyCommodityUtil(model,checkboxpid,commodityCount);

        //跳转去我的订单界面，然后在session中清空选中的商品
       if(!checkboxpid.equals("||")){
           Map<Integer,Integer> cars = getAllpid(session);
           String[] pids = checkboxpid.split("--");
           for (int i = 0; i < pids.length; i++) {
               cars.remove(Integer.parseInt(pids[i]));
           }
           log.debug("pids: " + checkboxpid + "cars的长度： " + cars.size());
           //将修改后的pids放回session中
           session.setAttribute("allpid",cars);
       }

       if(session.getAttribute("adminLoginUser") != null){
            return "adminPage/myOrder";
       }

        return "shopping/myOrder";
    }

    //处理直接从侧边栏点击去我的订单
    @RequestMapping(value = "/usr/ajax/toMyOrder",method = RequestMethod.GET)
    public String toMyOrder(Model model,HttpSession session) throws Exception {

        Map<Integer,Commodity> commodityMap = new HashMap<>();
        model.addAttribute("commodityMap",commodityMap);

        if (session.getAttribute("adminLoginUser") != null){
            return "adminPage/myOrder";
        }

        return "shopping/myOrder";
    }

    /**
     * 处理在二手界面直接点击购买请求
     * @param pid
     * @return
     */
    @RequestMapping(value = "/usr/ajax/buyImmediately/{pid}",method = RequestMethod.GET)
    public String buyImmediately(@PathVariable("pid") Integer pid,Model model) throws Exception {

        buyCommodityUtil(model,pid+"");

        return "shopping/myOrder";
    }

    /**
     * 处理立即购买和从购物车选择再购买，将它抽取为一个方法
     * @param model
     * @param args
     * @return
     * @throws Exception
     */
    public void buyCommodityUtil(Model model,String...args) throws Exception {

        int sumCount = 0;           //总共购买的商品数量
        double sumPrice = 0;        //总共价钱
        Map<Integer,Commodity> commodityMap = new HashMap<>();
        Map<Integer,Integer> counts = new HashMap<>();

        //处理立即购买
        if(args.length == 1){
            Commodity commodity = productService.findById(Integer.parseInt(args[0]));
            if (commodity != null) {
                commodityMap.put(commodity.getPid(), commodity);
                counts.put(commodity.getPid(), 1);
                sumPrice = commodity.getPrice();
                sumCount = 1;
                //从数据库中将商品数量减少一件
                Integer count = commodity.getCount() - 1;
                commodity.setCount(count);
                //更新商品数量
                productService.doUpdate(commodity);
            }
            //--:::7____二手华为手机____3____2:::--
            //- 商品编号:::7__商品名称:::二手华为手机__商品所属类别:::6__商品所属卖家:::2
            log.info("<===:::" + commodity.getPid() + "____" + commodity.getName() + "____" + commodity.getTid() +  "____" + commodity.getAuthorId() + ":::====>");
        }
        //处理从购物车中进行购买,第一个参数为pid的拼串，第二个参数为数量的拼串
        if(args.length == 2){
            if(!args[0].equals("||")) {

                String[] pids = args[0].split("--");
                String[] commodityCounts = args[1].split("__");

                for (int i = 0; i < pids.length; i++) {
                    try {
                        Commodity commodity = productService.findById(Integer.parseInt(pids[i]));
                        //判断是否在数据库中查询到了该商品
                        if (commodity != null) {
                            //从数据库中将商品数量减少一件
                            Integer count = commodity.getCount();
                            if(count >= Integer.parseInt(commodityCounts[i])){
                                commodity.setCount(count - Integer.parseInt(commodityCounts[i]));
                                counts.put(commodity.getPid(), Integer.parseInt(commodityCounts[i]));
                                sumPrice = sumPrice + commodity.getPrice() * Integer.parseInt(commodityCounts[i]);
                            }else {
                                //保存商品pid 和 购买的数量 作为key-value键值对,在前台通过pid来取得商品的购买数量
                                counts.put(commodity.getPid(), count);
                                sumPrice = sumPrice + commodity.getPrice() * count;
                                commodity.setCount(0);
                            }
                            commodityMap.put(commodity.getPid(), commodity);
                            sumCount++;

                            //更新商品数量
                            productService.doUpdate(commodity);
                            //将日志打印到文件中，用于后期做数据分析
                            log.info("<===:::" + commodity.getPid() + "____" + commodity.getName() + "____" + commodity.getTid() +  "____" + commodity.getAuthorId() + ":::====>");
                        }
                    } catch (Exception e) {
                        throw new Exception("数据转换异常！！！");
                    }
                }
            }
        }
        model.addAttribute("commodityMap",commodityMap);
        model.addAttribute("counts",counts);
        model.addAttribute("sumCount",sumCount);
        model.addAttribute("sumPrice",sumPrice);
    }

    /**
     *  从购物车中取出一件已加入购物车的商品
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/usr/shopping/removeFromShoppingCar",method = RequestMethod.POST)
    public String removeFromShoppingCar(@RequestParam(value = "pid") Integer pid,HttpSession session){

        HashMap<Integer,Integer> allpid = getAllpid(session);
        allpid.remove(pid);

        session.setAttribute("allpid",allpid);

        //返回购物车界面
        return "true";
    }

    /**
     * 处理修改购物数量的请求
     * @param pid
     * @param count
     * @param session
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/usr/shopping/updateCommodityCount")
    public String modifyCommodityCount(@RequestParam(value = "pid") Integer pid,@RequestParam(value = "count") Integer count ,HttpSession session){

        HashMap<Integer,Integer> allpid =  getAllpid(session);
        //修改购物数量
        allpid.put(pid,count);
        return "true";
    }

    /**
     * 处理用户发布求购请求
     * @return
     */
    @GetMapping(value = "/usr/iwantbuy")
    public String iWantBuyPre(Model model){
        //从商品表中查询出所有类型,用于显示选择框
        List<CommodityType> allCommodityType = productService.findAllCommodityType();
        model.addAttribute("allCommodityType",allCommodityType);
        return "shopping/wantbuy";
    }

    @PostMapping(value = "/usr/iwantbuy")
    public String iWantBuy(Commodity commodity,
                           @RequestParam("file") MultipartFile file,
                           HttpSession session, Model model) throws Exception {
        String fileName = null;
        //如果有照片上传
        if(!file.isEmpty() && file.getSize() > 0){
            log.info("[文件类型] - [{}]", file.getContentType());
            log.info("[文件名称] - [{}]", file.getOriginalFilename());
            log.info("[文件大小] - [{}]", file.getSize());
            //TODO 将文件写入到指定目录（具体开发中有可能是将文件写入到云存储/或者指定目录通过 Nginx
            //进行 gzip 压缩和反向代理，此处只是为了演示故将地址写成本地电脑指定目录）
            fileName = ipTimeStamp.getIPTimeRand() + ".jpg";     //重新生成图片名称，保证不重复
            file.transferTo(new File("E:\\IDEAWorkSpace\\GraduationProject\\gruaduation-project1\\web\\src\\main\\resources\\static\\images\\" + fileName));

        }
        //取得session域中的用户信息
        Customer customer = (Customer) session.getAttribute("customer");

        //设置要保存到数据库里面的照片名称
        commodity.setPhoto(fileName);

        //设置用户商品上传的日期
        String date = UserUtils.dateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        commodity.setDate(date);
        commodity.setFlag(1);           //设置商品的状态为求购的商品

        //设置该商品所属的用户
        commodity.setAuthorId(customer.getUid() + "");
        boolean flag = productAndUserService.addCommodity(customer, commodity);
        if(flag){
            model.addAttribute("msg","发布成功！");
        }else{
            model.addAttribute("msg","发布失败！");
        }
        //从商品表中查询出所有类型,用于显示选择框
        List<CommodityType> allCommodityType = productService.findAllCommodityType();
        model.addAttribute("allCommodityType",allCommodityType);

        return "shopping/wantbuy";
    }

    /**
     * 查看所有的求购信息
     * @return
     */
    @GetMapping(value = "/user/viewwantbuy")
    public String viewWantBuy(@RequestParam(defaultValue = "1") int pageNo,
                              @RequestParam(defaultValue = "15") int pageSize,
                              Model model,
                              HttpSession session){
        PageHelper.startPage(pageNo, pageSize,true);
        Page<Commodity> commodities = productService.viewWantBuy();
        PageInfo<Commodity> page = new PageInfo<>(commodities,5);
        model.addAttribute("pageInfo",page);

        return "shopping/viewwantbuy";
    }

}
