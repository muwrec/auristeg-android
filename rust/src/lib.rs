use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::{jbyteArray, jint, jlong};
use jni::JNIEnv;
use std::ffi::c_void;

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{LazyLock, Mutex};

static NEXT_HANDLE: AtomicU64 = AtomicU64::new(1);
static IMAGE_CACHE: LazyLock<Mutex<HashMap<u64, DecodedImage>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

struct DecodedImage {
    rgba: Vec<u8>,
    width: u32,
    height: u32,
}

fn image_to_rgba(img: &image::DynamicImage) -> DecodedImage {
    let rgba_img = img.to_rgba8();
    let (width, height) = rgba_img.dimensions();
    DecodedImage {
        width,
        height,
        rgba: rgba_img.into_raw(),
    }
}

fn u8_as_i8(data: &[u8]) -> &[i8] {
    unsafe { &*(data as *const [u8] as *const [i8]) }
}

fn extract_bit(raw: &[u8], w: u32, h: u32, channel: usize, bit: usize) -> Vec<u8> {
    let len = (w * h) as usize;
    let mut out = vec![0u8; len];
    for i in 0..len {
        out[i] = if (raw[i * 4 + channel] >> bit) & 1 == 1 {
            255
        } else {
            0
        };
    }
    out
}

#[no_mangle]
pub extern "C" fn Java_com_auristeg_android_RustBridge_loadImage(
    env: JNIEnv,
    _class: JClass,
    bytes: jbyteArray,
) -> jlong {
    let jba = unsafe { JByteArray::from(JObject::from_raw(bytes as *mut _)) };
    let data = match env.convert_byte_array(jba) {
        Ok(d) => d,
        Err(_) => return -1,
    };
    let img = match image::load_from_memory(&data) {
        Ok(i) => i,
        Err(_) => return -1,
    };
    let decoded = image_to_rgba(&img);
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    IMAGE_CACHE.lock().unwrap().insert(handle, decoded);
    handle as jlong
}

#[no_mangle]
pub extern "C" fn Java_com_auristeg_android_RustBridge_getWidth(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    IMAGE_CACHE
        .lock()
        .unwrap()
        .get(&(handle as u64))
        .map(|d| d.width as jint)
        .unwrap_or(0)
}

#[no_mangle]
pub extern "C" fn Java_com_auristeg_android_RustBridge_getHeight(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jint {
    IMAGE_CACHE
        .lock()
        .unwrap()
        .get(&(handle as u64))
        .map(|d| d.height as jint)
        .unwrap_or(0)
}

#[no_mangle]
pub extern "C" fn Java_com_auristeg_android_RustBridge_getRgba(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jbyteArray {
    let (ptr, len) = {
        let cache = IMAGE_CACHE.lock().unwrap();
        match cache.get(&(handle as u64)) {
            Some(d) => (d.rgba.as_ptr(), d.rgba.len()),
            None => return std::ptr::null_mut(),
        }
    };
    let arr = env.new_byte_array(len as jint).unwrap();
    let buf = unsafe { std::slice::from_raw_parts(ptr as *const i8, len) };
    let _ = env.set_byte_array_region(&arr, 0, buf);
    arr.into_raw()
}

#[no_mangle]
pub extern "C" fn Java_com_auristeg_android_RustBridge_extractBitPlane(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    channel: jint,
    bit: jint,
) -> jbyteArray {
    let plane = {
        let cache = IMAGE_CACHE.lock().unwrap();
        match cache.get(&(handle as u64)) {
            Some(d) => extract_bit(&d.rgba, d.width, d.height, channel as usize, bit as usize),
            None => return std::ptr::null_mut(),
        }
    };
    let arr = env.new_byte_array(plane.len() as jint).unwrap();
    let buf = u8_as_i8(&plane);
    let _ = env.set_byte_array_region(&arr, 0, buf);
    arr.into_raw()
}

#[link(name = "jnigraphics")]
extern "C" {
    fn AndroidBitmap_lockPixels(
        env: *mut jni::sys::JNIEnv,
        bitmap: jni::sys::jobject,
        addr: *mut *mut c_void,
    ) -> i32;
    fn AndroidBitmap_unlockPixels(
        env: *mut jni::sys::JNIEnv,
        bitmap: jni::sys::jobject,
    ) -> i32;
}

#[no_mangle]
pub extern "C" fn Java_com_auristeg_android_RustBridge_fillBitmap(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    bitmap: JObject,
) -> jint {
    let native_env = env.get_native_interface();
    let mut pixels: *mut c_void = std::ptr::null_mut();
    let ret = unsafe { AndroidBitmap_lockPixels(native_env, bitmap.as_raw(), &mut pixels) };
    if ret != 0 {
        return ret as jint;
    }
    let result = {
        let cache = IMAGE_CACHE.lock().unwrap();
        match cache.get(&(handle as u64)) {
            Some(d) => {
                unsafe {
                    std::ptr::copy_nonoverlapping(d.rgba.as_ptr(), pixels as *mut u8, d.rgba.len());
                }
                0i32
            }
            None => -1i32,
        }
    };
    unsafe { AndroidBitmap_unlockPixels(native_env, bitmap.as_raw()) };
    result
}

#[no_mangle]
pub extern "C" fn Java_com_auristeg_android_RustBridge_freeImage(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    IMAGE_CACHE.lock().unwrap().remove(&(handle as u64));
}
