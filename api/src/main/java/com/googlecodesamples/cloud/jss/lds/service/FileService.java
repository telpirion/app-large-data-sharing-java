package com.googlecodesamples.cloud.jss.lds.service;

import com.googlecodesamples.cloud.jss.lds.model.BaseFile;
import com.googlecodesamples.cloud.jss.lds.model.FileMeta;
import com.googlecodesamples.cloud.jss.lds.util.LdsUtil;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
  private static final Logger log = LoggerFactory.getLogger(FirestoreService.class);
  private final FirestoreService firestoreService;
  private final StorageService storageService;

  @Value("${resource.path}")
  private String basePath;

  public FileService(FirestoreService firestoreService, StorageService storageService) {
    this.firestoreService = firestoreService;
    this.storageService = storageService;
  }

  /**
   * Upload files to Firestore and Cloud Storage.
   *
   * @param files list of files upload to the server
   * @param tags list of tags label the files
   * @return list of files data
   */
  public List<BaseFile> uploadFiles(List<MultipartFile> files, List<String> tags) throws Exception {
    log.trace("entering uploadFiles()");
    List<BaseFile> fileList = new ArrayList<>();
    for (MultipartFile file : files) {
      String fileId = LdsUtil.generateUuid();
      BaseFile newFile = createOrUpdateFile(file, tags, fileId, fileId, file.getSize());
      fileList.add(newFile);
    }
    return fileList;
  }

  /**
   * Update a file to Firestore and Cloud Storage.
   *
   * @param newFile new file upload to the server
   * @param tags list of tags label the new file
   * @param file previously uploaded file
   * @return file data
   */
  public BaseFile updateFile(MultipartFile newFile, List<String> tags, BaseFile file)
      throws Exception {
    log.trace("entering updateFile()");
    String fileId = file.getId();
    if (newFile == null) {
      String pathId = LdsUtil.getPathId(file.getPath());
      return createOrUpdateFileMeta(tags, fileId, pathId, file.getName(), file.getSize());
    }
    storageService.delete(file.getPath());
    storageService.delete(file.getThumbnailPath());
    String newFileId = LdsUtil.generateUuid();
    return createOrUpdateFile(newFile, tags, fileId, newFileId, newFile.getSize());
  }

  /**
   * Delete a file from Firestore and Cloud Storage.
   *
   * @param file uploaded file
   */
  public void deleteFile(BaseFile file) {
    log.trace("entering deleteFile()");
    firestoreService.delete(file.getId());
    storageService.delete(file.getPath());
    storageService.delete(file.getThumbnailPath());
  }

  /**
   * Search files with given tags.
   *
   * @param tags list of tags label the files
   * @param orderNo application defined column for referencing order
   * @param size number of files return
   * @return list of files data
   */
  public List<BaseFile> getFilesByTag(List<String> tags, String orderNo, int size)
      throws Exception {
    log.trace("entering getFilesByTag()");
    return firestoreService.getFilesByTag(tags, orderNo, size);
  }

  /**
   * Search a file with given fileId.
   *
   * @param fileId unique id of the file
   * @return file data
   */
  public BaseFile getFileById(String fileId) throws Exception {
    log.trace("entering getFileById()");
    return firestoreService.getFileById(fileId);
  }

  /** Delete all files from Firestore and Cloud Storage. */
  public void resetFile() {
    log.trace("entering resetFile()");
    firestoreService.deleteCollection();
    storageService.batchDelete();
  }

  /**
   * Create or update a file in Cloud Storage with the given fileId.
   *
   * @param file file upload to the server
   * @param tags list of tags label the file
   * @param fileId unique id of the file
   * @param newFileId unique id of the new file (for referencing Cloud Storage)
   * @param size size of the file
   * @return file data
   */
  private BaseFile createOrUpdateFile(
      MultipartFile file, List<String> tags, String fileId, String newFileId, long size)
      throws Exception {
    BaseFile newFile =
        createOrUpdateFileMeta(tags, fileId, newFileId, file.getOriginalFilename(), size);
    storageService.save(newFile.getPath(), file.getContentType(), file.getBytes());
    if (newFile.isImage()) {
      createThumbnail(file, newFile.getThumbnailPath());
    }
    return newFile;
  }

  /**
   * Create or update the metadata of a file in Firestore with the given fileId.
   *
   * @param tags list of tags label the file
   * @param fileId unique id of the file
   * @param newFileId unique id of the new file (for referencing Cloud Storage)
   * @param fileName name of the file
   * @param size size of the file
   * @return file data
   */
  private BaseFile createOrUpdateFileMeta(
      List<String> tags, String fileId, String newFileId, String fileName, long size)
      throws Exception {
    String fileBucketPath = LdsUtil.getFileBucketPath(basePath, newFileId);
    FileMeta fileMeta = new FileMeta(fileId, fileBucketPath, fileName, tags, size);
    firestoreService.save(fileMeta);
    return getFileById(fileId);
  }

  /**
   * Create a thumbnail of the given file.
   *
   * @param file file to create thumbnail
   * @param thumbnailId unique id of the thumbnail file
   */
  private void createThumbnail(MultipartFile file, String thumbnailId) throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Thumbnails.of(file.getInputStream())
        .size(300, 300)
        .keepAspectRatio(false)
        .toOutputStream(byteArrayOutputStream);
    storageService.save(thumbnailId, file.getContentType(), byteArrayOutputStream.toByteArray());
  }
}
