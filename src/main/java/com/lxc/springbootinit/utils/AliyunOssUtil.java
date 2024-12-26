package com.lxc.springbootinit.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import com.lxc.springbootinit.config.CosClientConfig;
import com.lxc.springbootinit.constant.FileConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 封装文件上传方法
 */
@Component
public class AliyunOssUtil {
	@Resource
	private CosClientConfig config;

	@Resource
	private OSS ossClient;// 注入阿里云oss文件服务器客户端

	/**
	 * 文件上传
	 *
	 * @param file
	 * @return
	 */

	public String upload(File file) {
		if (file == null) {
			return null;
		}
		String endPoint = config.getEndPoint();
		String keyId = config.getAccessKeyId();
		String keySecret = config.getAccessKeySecret();
		String bucketName = config.getBucketName();
		String fileHost = config.getFileHost();

		//定义子文件的格式
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String dateStr = format.format(new Date());
		//阿里云文件上传客户端
		OSSClient client = new OSSClient(endPoint, keyId, keySecret);
		try {
			//判断桶是否存在
			if (!client.doesBucketExist(bucketName)) {
				//创建桶
				client.createBucket(bucketName);
				CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
				//设置访问权限为公共读
				createBucketRequest.setCannedACL(CannedAccessControlList.PublicRead);
				//发起创建桶的请求
				client.createBucket(createBucketRequest);
			}
			//当桶存在时,进行文件上传
			//设置文件路径和名称
			String fileUrl = fileHost + "/" + (dateStr + "/" + UUID.randomUUID().toString().replace("-", "") + "-" + file.getName());
			PutObjectResult result = client.putObject(new PutObjectRequest(bucketName, fileUrl, file));
			client.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
			//文件上传成功后,返回当前文件的路径
			if (result != null) {
				// 加上我们阿里云的前缀，可以通过url访问和下载
//				return FileConstant.COS_PREFIX + fileUrl;
				// 这里我们不显示完整的地址了，就用 file/xxx.png 这样的格式返回给前端（可以配合文件下载）
				return fileUrl;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (client != null) {
				client.shutdown();
			}
		}
		return null;
	}

	public void fileDownLoad(String filePath, HttpServletResponse response) throws UnsupportedEncodingException {

		// 设置响应头，通知浏览器进行文件下载
		response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filePath, "UTF-8"));
		response.setContentType("application/octet-stream"); // 设置内容类型为二进制流

		String bucketName = config.getBucketName();

		try (OSSObject ossObject = ossClient.getObject(bucketName, filePath);
			 InputStream inputStream = new BufferedInputStream(ossObject.getObjectContent());
			 OutputStream outputStream = new BufferedOutputStream(response.getOutputStream())) {

			// 定义缓冲区
			byte[] buffer = new byte[1024];
			int len;

			// 读取文件内容并写入响应流
			while ((len = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, len);
			}

			outputStream.flush(); // 确保数据完全写出

		} catch (Exception e) {
			e.printStackTrace(); // 打印错误日志（生产环境可换成记录日志）
			throw new RuntimeException("文件下载失败");
		}
	}
}