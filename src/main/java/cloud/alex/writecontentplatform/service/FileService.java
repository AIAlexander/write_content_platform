package cloud.alex.writecontentplatform.service;

import cloud.alex.writecontentplatform.model.dto.image.ImageDto;

/**
 * @author wangshuhao
 * @date 2026/4/8
 */
public interface FileService {


    String uploadLocalFile(String localPath, String ossName);

    String uploadImage(ImageDto imageDto, String folder);

    String getFileUrl(String ossName);

}
