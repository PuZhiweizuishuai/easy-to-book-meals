<template>
  <div class="image-upload-component w-full">
    <!-- 裁剪弹窗 Modal (使用 DaisyUI) -->
    <dialog ref="cropperModal" class="modal modal-middle">
      <div class="modal-box w-11/12 max-w-3xl">
        <h3 class="font-bold text-lg mb-4">调整图片裁剪区域</h3>
        <div class="cropper-container w-full h-80 bg-base-200 rounded-md overflow-hidden">
          <img ref="cropperImage" :src="currentImageUrl" class="max-w-full" />
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="cancelCrop">取消</button>
          <button class="btn btn-primary" @click="confirmCrop">确认裁剪</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">close</form>
    </dialog>

    <!-- FilePond 组件 -->
    <div class="form-control w-full">
      <file-pond ref="pond" name="files[]" label-idle='拖拽图片至此处 或 <span class="filepond--label-action">点击选择</span>'
        label-file-processing-complete="文件上传成功！" label-tap-to-retry="尝试重试" label-file-processing="文件正在上传，请稍后"
        label-file-processing-aborted="文件上传被取消" :allow-multiple="allowMultiple" :max-files="allowMultiple ? 10 : 1"
        accepted-file-types="image/jpeg, image/png, image/gif, image/webp" :server="serverConfig"
        :instant-upload="false" @init="handleFilePondInit" @addfile="handleAddFile" @removefile="handleRemoveFile"
        @processfile="handleProcessFile" @error="handleError" />

      <!-- 手动上传按钮 -->
      <button v-if="hasPendingFiles" class="btn btn-primary btn-block mt-4" @click="triggerUpload">
        开始上传
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from "vue";
import vueFilePond from "vue-filepond";

// Import FilePond styles
import "filepond/dist/filepond.min.css";
import "filepond-plugin-image-preview/dist/filepond-plugin-image-preview.min.css";

// Import FilePond plugins (移除了自动裁剪插件，改用手动 Cropper)
import FilePondPluginFileValidateType from "filepond-plugin-file-validate-type";
import FilePondPluginFileValidateSize from "filepond-plugin-file-validate-size";
import FilePondPluginImagePreview from "filepond-plugin-image-preview";

// Import Cropper.js
import Cropper from "cropperjs";
import "cropperjs/dist/cropper.css";

// Create FilePond component
const FilePond = vueFilePond(
  FilePondPluginFileValidateType,
  FilePondPluginFileValidateSize,
  FilePondPluginImagePreview
);

const props = defineProps({
  aspectRatio: {
    type: String,
    default: "1:1",
  },
  allowMultiple: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["upload-success", "upload-error", "remove-file"]);

const pond = ref(null);
const cropperModal = ref(null);
const cropperImage = ref(null);
const currentImageUrl = ref("");
const currentFileId = ref(null);
let cropperInstance = null;
let isReplacing = false; // 防止替换文件时再次触发裁剪弹窗

// 用于建立 FilePond 文件本地 id 与服务器返回数据的映射关系
const fileServerDataMap = new Map();

// 计算是否有待上传文件（用于显示手动上传按钮）
const hasPendingFiles = computed(() => {
  if (!pond.value) return false;
  const files = pond.value.getFiles();
  return files.some(f => f.status !== 5); // status 5 是已上传完毕
});

// 配置服务器
// 配置服务器上传地址及处理逻辑（接管 process 以移除多余元数据）
const serverConfig = {
  process: (fieldName, file, metadata, load, error, progress, abort) => {
    // 1. 手动创建干净的 FormData，只包含文件，不包含 metadata
    const formData = new FormData();
    formData.append('files[]', file, file.name);

    // 2. 使用原生 XMLHttpRequest 发起请求，以便手动控制进度和取消
    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/api/files/upload');

    // 携带 Sa-Token 认证头（后端 /api/files/upload 需要登录）
    const token = localStorage.getItem('token');
    if (token) {
      xhr.setRequestHeader('Token', token);
    }

    // 监听上传进度，驱动 FilePond 的进度条
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        progress(e.loaded / e.total * 100);
      }
    };

    // 监听请求完成
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const response = JSON.parse(xhr.responseText);
          // 以稳定的本地文件 file.id 极为键，缓存后端返回的数据对象
          emit("upload-success", response);
          // 必须调用 load 通知 FilePond 该文件上传成功
          load(xhr.responseText);
        } catch (e) {
          
          emit("upload-success", xhr.responseText);
          load(xhr.responseText);
        }
      } else {
        let errorData = xhr.responseText;
        try { errorData = JSON.parse(xhr.responseText); } catch (e) { }
        emit("upload-error", errorData);
        // 必须调用 error 通知 FilePond 该文件上传失败
        error('Upload failed');
      }
    };

    xhr.send(formData);

    // 返回一个包含 abort 方法的对象，允许 FilePond 取消上传
    return {
      abort: () => {
        xhr.abort();
      }
    };
  },
  revert: null,
  restore: null,
  load: null,
  fetch: null,
};

// 当文件从 FilePond 中移除时触发
const handleRemoveFile = (error, file) => {
  console.log('移除文件:', file);
  if (!error && file) {
    // 修改：1. 优先从我们本地维护的 Map 中通过稳定的 file.id 获取服务器响应数据
    let fileData = fileServerDataMap.get(file.id) || [];
    console.log(fileServerDataMap);
    
    // 2. 兜底逻辑：如果 Map 中没拿到（例如未上传就删除了），再尝试直接解析 file.serverId
    if (fileData.length === 0 && file.serverId) {
      try {
        const response = JSON.parse(serverId);
        if (response?.data && Array.isArray(response.data)) {
          fileData = response.data;
        }
      } catch (e) {
        console.error('解析 serverId 失败:', e);
      }
    }
    
    // 新增：3. 从 Map 中移除，防止内存泄漏
    fileServerDataMap.delete(file.id);
    
    // 触发传递给父组件
    emit("remove-file", { fileData });
  }
};

const handleFilePondInit = () => {
  console.log("FilePond has initialized");
};

// 当文件被添加到 FilePond 时触发
const handleAddFile = (error, file) => {
  if (error) return;

  // 如果是内部替换操作触发的 addfile，不弹窗
  if (isReplacing) {
    isReplacing = false;
    return;
  }

  // 如果是图片类型，弹出裁剪框
  if (file.fileType && file.fileType.includes('image')) {
    currentFileId.value = file.id;
    currentImageUrl.value = URL.createObjectURL(file.file);
    openCropperModal();
  }
};

const openCropperModal = () => {
  if (cropperModal.value) {
    cropperModal.value.showModal();
    // 延迟初始化 Cropper，确保图片和弹窗已渲染
    setTimeout(initCropper, 200);
  }
};

const initCropper = () => {
  if (cropperInstance) {
    cropperInstance.destroy();
  }

  // 解析比例字符串 '16:9' -> 16/9
  const ratioParts = props.aspectRatio.split(':');
  const aspectRatio = Number(ratioParts[0]) / Number(ratioParts[1]);

  cropperInstance = new Cropper(cropperImage.value, {
    aspectRatio: aspectRatio,
    viewMode: 1, // 限制裁剪框不超出图片
    autoCropArea: 1, // 默认选中全图
    responsive: true,
  });
};

// 确认裁剪
const confirmCrop = () => {
  if (!cropperInstance) return;

  // 获取裁剪后的 canvas
  const canvas = cropperInstance.getCroppedCanvas({
    maxWidth: 2048,
    maxHeight: 2048,
    fillColor: '#fff',
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high',
  });

  // 将 Canvas 转换为 Blob
  canvas.toBlob((blob) => {
    // 将 Blob 包装成 File 对象，并尽量保持原名
    const croppedFile = new File([blob], `cropped_${Date.now()}.jpg`, { type: 'image/jpeg' });

    // 从 FilePond 中移除原文件
    if (currentFileId.value) {
      isReplacing = true; // 标记为替换中，防止再次触发 handleAddFile
      pond.value.removeFile(currentFileId.value);
    }

    // 将裁剪后的新文件添加到 FilePond
    pond.value.addFile(croppedFile).then(() => {
      // 添加成功后关闭弹窗
      closeCropperModal();
    });
  }, 'image/jpeg', 0.9);
};

// 取消裁剪
const cancelCrop = () => {
  // 如果取消，移除未裁剪的原图
  if (currentFileId.value) {
    pond.value.removeFile(currentFileId.value);
  }
  closeCropperModal();
};

const closeCropperModal = () => {
  if (cropperInstance) {
    cropperInstance.destroy();
    cropperInstance = null;
  }
  if (currentImageUrl.value) {
    URL.revokeObjectURL(currentImageUrl.value); // 释放内存
  }
  currentImageUrl.value = "";
  currentFileId.value = null;
  if (cropperModal.value) {
    cropperModal.value.close();
  }
};

// 手动触发上传
const triggerUpload = () => {
  if (pond.value) {
    pond.value.processFiles();
  }
};

const handleProcessFile = (error, file) => {
  if (!error && file) {
    console.log("File uploaded successfully", file);
    
    // 新增：当上传成功，file.serverId 包含的是我们在 server.process 的 load() 中传入的响应文本
    if (file.serverId) {
      try {
        const response = JSON.parse(file.serverId);
        if (response?.data && Array.isArray(response.data)) {
          // 以稳定的本地文件 file.id 极为键，缓存后端返回的数据对象
          fileServerDataMap.set(file.id, response.data);
        }
      } catch (e) {
        console.error("缓存服务器响应数据失败:", e);
      }
    }
  }
};

const handleError = (error, file, status) => {
  console.error("File upload error", error, status);
};

// 组件销毁时清理 Cropper 实例
onBeforeUnmount(() => {
  if (cropperInstance) {
    cropperInstance.destroy();
  }
});
</script>

<style scoped>
/* DaisyUI 主题色适配 FilePond */
:deep(.filepond--panel-root) {
  background-color: oklch(var(--b2));
  border: 2px dashed oklch(var(--bc) / 0.2);
  border-radius: var(--rounded-box);
}

:deep(.filepond--drop-label) {
  color: oklch(var(--bc) / 0.6);
}

:deep(.filepond--label-action) {
  color: oklch(var(--p));
  text-decoration: underline;
}

:deep(.filepond--file-action-button) {
  background-color: oklch(var(--p));
  color: oklch(var(--pc));
  border-radius: var(--rounded-btn);
}

/* 确保 Cropper 弹窗中的图片能被正确挂载 */
.cropper-container img {
  display: block;
  max-width: 100%;
}
</style>