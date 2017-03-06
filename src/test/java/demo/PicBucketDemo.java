package demo;


import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

import java.io.File;
import java.io.IOException;

/**
 * 图片类空间的demo，一般性操作参考文件空间的demo（FileBucketDemo.java）
 * <p>
 * 注意：直接使用部分图片处理功能后，将会丢弃原图保存处理后的图片
 */
public class PicBucketDemo {

    // 运行前先设置好以下四个参数
    private static final String BUCKET_NAME = "your bucket name";
    private static final String OPERATOR_NAME = "your access key";
    private static final String OPERATOR_PWD = "your secret key";
    private static final String URL = "你的七牛域名";


    /**
     * 根目录
     */
    private static final String DIR_ROOT = "/";

    /**
     * 上传到qiniu的图片名
     */
    private static final String PIC_NAME = "sample.jpeg";

    /**
     * 本地待上传的测试文件
     */
    private static final String SAMPLE_PIC_FILE = System
            .getProperty("user.dir") + "/sample.jpeg";

    private static Auth auth = null;

    private static UploadManager uploadManager = null;

    private static BucketManager bucketManager = null;

    private static String upToken = null;

    static {
        File picFile = new File(SAMPLE_PIC_FILE);

        if (!picFile.isFile()) {
            System.out.println("本地待上传的测试文件不存在!");
        }
    }

    public static void main(String[] args) throws Exception {

        //构造一个带指定Zone对象的配置类
        Configuration cfg = new Configuration(Zone.autoZone());
        uploadManager = new UploadManager(cfg);

        // 初始化空间
        auth = Auth.create(OPERATOR_NAME, OPERATOR_PWD);
        bucketManager = new BucketManager(auth, cfg);
        upToken = auth.uploadToken(BUCKET_NAME);

        testWriteFile(null);

//        testDeleteFile("FmDy9JzR7P2UkXKmkTASAGL0NRA5");
    }

    /**
     * 上传文件
     *
     * @throws IOException
     */
    public static void testWriteFile(String fileName) throws IOException {


        fileName = fileName == null ? null : "sample.jpeg";
        try {
            Response response = PicBucketDemo.uploadManager.put(SAMPLE_PIC_FILE, fileName, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            System.out.println(putRet.key);
            System.out.println(putRet.hash);
        } catch (QiniuException ex) {
            Response r = ex.response;
            System.err.println(r.toString());
            try {
                System.err.println(r.bodyString());
            } catch (QiniuException ex2) {
                //ignore
            }
        }
    }

    public static void testDeleteFile(String fileName) throws IOException {


        try {
            bucketManager.delete(BUCKET_NAME, fileName);
        } catch (QiniuException ex) {
            //如果遇到异常，说明删除失败
            System.err.println(ex.code());
            System.err.println(ex.response.toString());
        }

    }



    private static String isSuccess(boolean result) {
        return result ? " 成功" : " 失败";
    }
}
