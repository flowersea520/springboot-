package com.lxc.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.lxc.springbootinit.common.BaseResponse;
import com.lxc.springbootinit.common.ErrorCode;
import com.lxc.springbootinit.common.ResultUtils;
import com.lxc.springbootinit.constant.FileConstant;
import com.lxc.springbootinit.exception.BusinessException;
import com.lxc.springbootinit.model.dto.file.UploadFileRequest;
import com.lxc.springbootinit.model.entity.User;
import com.lxc.springbootinit.model.enums.FileUploadBizEnum;
import com.lxc.springbootinit.service.UserService;

import java.io.*;
import java.net.URLEncoder;
import java.util.Arrays;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lxc.springbootinit.utils.AliyunOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口
 *
 * @author <a href="https://github.com/flowersea520">程序员鱼皮</a>
 * @from <a href="https://lxc.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

	@Resource
	private UserService userService;


	@Resource
	private AliyunOssUtil aliyunOssUtil;


	/**
	 * 文件上传
	 *
	 * @param multipartFile
	 * @param uploadFileRequest
	 * @param request
	 * @return
	 */
	@PostMapping("/uploadFile")
	public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
										   UploadFileRequest uploadFileRequest, HttpServletRequest request) {
		// 获取业务类型
		String biz = uploadFileRequest.getBiz();
		FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);

		if (fileUploadBizEnum == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}

		// 验证文件类型（可以根据需求进行扩展）
		validFile(multipartFile, fileUploadBizEnum);

		// 获取当前登录用户（用于区分不同用户的上传目录）
		User loginUser = userService.getLoginUser(request);
		if (ObjectUtil.isEmpty(loginUser)) {
			throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
		}

		String uploadUrl = null;
		File tempFile = null;  // 定义一个临时文件变量
		try {
			if (multipartFile != null) {
				String fileName = multipartFile.getOriginalFilename();
				if (fileName != null && !"".equals(fileName.trim())) {
					// 清理文件名，避免特殊字符问题
					fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

					// 创建临时目录，确保文件不会出现在项目根目录
					File tempDir = new File(System.getProperty("java.io.tmpdir"));
					tempFile = new File(tempDir, fileName);

					// 使用 transferTo 代替 FileOutputStream 来避免重复写入
					multipartFile.transferTo(tempFile);

					// 上传文件
					uploadUrl = aliyunOssUtil.upload(tempFile);
					log.info("Uploaded file, path= {}", uploadUrl);
				}
			}
		} catch (Exception e) {
			log.error("File upload error, filepath = {}", tempFile != null ? tempFile.getPath() : "unknown", e);
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
		} finally {
			// 删除临时文件
			if (tempFile != null && tempFile.exists()) {
				boolean delete = tempFile.delete();
				if (!delete) {
					log.error("File delete error, filepath = {}", tempFile.getPath());
				}
			}
		}
		return ResultUtils.success(uploadUrl);
	}

	/**
	 * 文件下载
	 * 我们文件上传后的url时：file/2024-12-26/a447a7effcb84d019e436d83f9aa47ca-11.png用这个作为filepath
	 *
	 * @param filePath 文件名称
	 * @param response HTTP响应对象
	 */
	@PostMapping("/download")
	public void download(String filePath, HttpServletResponse response) throws UnsupportedEncodingException {
		aliyunOssUtil.fileDownLoad(filePath, response);
	}


	/**
	 * 校验文件
	 *
	 * @param multipartFile
	 * @param fileUploadBizEnum 业务类型
	 */
	private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
		// 文件大小
		long fileSize = multipartFile.getSize();
		// 文件后缀
		String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
		final long TEN_M = 10 * 1024 * 1024L;
		final long FIFTY_M = 50 * 1024 * 1024L;
		// 用户上传的文件校验
		// 如果是用户上传所 对应的 逻辑校验
		if (FileUploadBizEnum.UPLOAD_FILE.equals(fileUploadBizEnum)) {
			if (fileSize > FIFTY_M) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 50M");
			}
			if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp", "pdf", "txt").contains(fileSuffix)) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
			}
		}

		// 如果是用户上传所 对应的 逻辑校验
		if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
			if (fileSize > TEN_M) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 10M");
			}
			if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
			}
		}
	}
}
