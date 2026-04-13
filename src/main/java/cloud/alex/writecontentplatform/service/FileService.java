package cloud.alex.writecontentplatform.service;

/**
 * @author wangshuhao
 * @date 2026/4/8
 */
public interface FileService {


    String uploadLocalFile(String localPath, String ossName);

    String getFileUrl(String ossName);

}
