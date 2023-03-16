package com.itheima.controller;


import com.itheima.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/common")
public class CommonCoontroller {

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 上传
     *
     * @param file 文件
     * @return {@link R}<{@link String}>
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) {
        //原始的文件名称，获得后缀名称
        String subs = file.getOriginalFilename().substring(file.getOriginalFilename().indexOf("."));

        //使用uuid生成文件名，防止文件名重复造成文件覆盖
        String filename = UUID.randomUUID().toString() + subs;

        //创建一个目录
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(basePath+filename));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return R.success(filename);
    }
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){

        try {
            //通过输入流读取文件内容
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));

            //通过输出流写回浏览器进行显示数据
            ServletOutputStream outputStream = response.getOutputStream();
            //设置浏览器响应回去文件格式
            response.setContentType("image/jpeg");
            int len=0;
            byte[] bytes=new byte[1024];
            while ((len=fileInputStream.read(bytes))!=-1){
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }
            //关闭流
            outputStream.close();
            fileInputStream.close();

        } catch (Exception e) {
          e.printStackTrace();
        }

    }


}
