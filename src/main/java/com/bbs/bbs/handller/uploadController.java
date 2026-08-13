package com.bbs.bbs.handller;

import com.bbs.bbs.common.Constants;
import com.bbs.bbs.common.ServiceResultEnum;
import com.bbs.bbs.upload.UploadStorage;
import com.bbs.util.Result;
import com.bbs.util.ResultGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import com.bbs.util.SystemUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-24 18:40:52
 * @Version 1.0
 */
@Controller
public class uploadController {
    private final UploadStorage uploadStorage;

    public uploadController(UploadStorage uploadStorage) {
        this.uploadStorage = uploadStorage;
    }

    @PostMapping("/uploadFile")
    @ResponseBody
    //单文件上传
    public Result<String> upload(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            return ResultGenerator.genFailResult("文件大小超过限制");
        }
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (StringUtils.hasLength(originalFilename)) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            if (!Constants.ALLOWED_EXTENSIONS.contains(suffix)) {
                return ResultGenerator.genFailResult(ServiceResultEnum.SYSTEM_ERROR.getResult() + "文件类型错误");
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String newFileName = sdf.format(new Date()) + UUID.randomUUID() + "_" + suffix;
        try {
            uploadStorage.store(file, newFileName);
            // 构建可访问的 URL 并返回
            Result<String> resultSuccess = ResultGenerator.genSuccessResult();
            resultSuccess.setData(SystemUtil.getHost(new URI(request.getRequestURL() + "")) + "/upload/" + newFileName);
            return resultSuccess;
        } catch (Exception e) {
            e.printStackTrace();
            return ResultGenerator.genFailResult(ServiceResultEnum.SYSTEM_ERROR.getResult() + "上传文件失败");
        }
    }

    //多文件上传
    @PostMapping("/uploadFiles")
    @ResponseBody
    public Result<List<String>> uploadFiles(HttpServletRequest request, @RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResultGenerator.genFailResult("请选择文件");
        }
        if (files.size() > 5) {
            return ResultGenerator.genFailResult("最多上传5个文件");
        }
        List<String> urls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                    return ResultGenerator.genFailResult("文件大小超过限制");
                }
                String originalFilename = file.getOriginalFilename();
                String suffix = "";
                if (StringUtils.hasLength(originalFilename)) {
                    suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
                    if (!Constants.ALLOWED_EXTENSIONS.contains(suffix)) {
                        return ResultGenerator.genFailResult(ServiceResultEnum.SYSTEM_ERROR.getResult() + "文件类型错误");
                    }
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String newFileName = sdf.format(new Date()) + UUID.randomUUID() + "_" + suffix;
                uploadStorage.store(file, newFileName);
                // 构建可访问的 URL
                String url = SystemUtil.getHost(new URI(request.getRequestURL() + "")) + "/upload/" + newFileName;
                urls.add(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultGenerator.genFailResult(ServiceResultEnum.SYSTEM_ERROR.getResult() + "上传文件失败");
        }
        Result<List<String>> resultSuccess = ResultGenerator.genSuccessResult();
        resultSuccess.setData(urls);
        return resultSuccess;
    }
}
