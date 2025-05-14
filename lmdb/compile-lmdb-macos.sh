#!/opt/homebrew/bin/bash

if [ ! -d "lmdb" ]; then
  git clone https://git.openldap.org/openldap/openldap.git lmdb
fi
cd ./lmdb/libraries/liblmdb || exit
git checkout LMDB_0.9.33

declare -A build_outputs
declare -A supported_targets=(
#  [iosArm64/liblmdb.dylib]="make CC='xcrun --sdk iphoneos --toolchain iphoneos clang -arch arm64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
#  [iossimulatorArm64/liblmdb.dylib]="make CC='xcrun --sdk iphonesimulator --toolchain iphoneos clang -arch arm64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
#  [iossimulatorX64/liblmdb.dylib]="make CC='xcrun --sdk iphonesimulator --toolchain iphoneos clang -arch x86_64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
#  [linuxArm64/liblmdb.so]="docker run --mount type=bind,source=$(pwd),target=/lmdb --rm --platform=linux/arm64 -w /lmdb gcc:latest make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
#  [linuxX64/liblmdb.so]="docker run --mount type=bind,source=$(pwd),target=/lmdb --rm --platform=linux/amd64 -w /lmdb gcc:latest make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
#  [macosArm64/liblmdb.dylib]="make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
#  [macosX64/liblmdb.dylib]="make CC='clang -mmacosx-version-min=10.15 -arch x86_64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
#  [mingwX64/lmdb.dll]="make CC='x86_64-w64-mingw32-gcc' AR='x86_64-w64-mingw32-gcc-ar' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
#  [androidNativeArm64/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
#  [androidNativeArm32/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/armv7a-linux-androideabi21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
#  [androidNativeX86/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/i686-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
#  [androidNativeX64/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/x86_64-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
  [wasmJs/liblmdb.wasm]="emcc -O3 mdb.c midl.c -o liblmdb.mjs -s WASM=1 -s EXPORT_ES6=1 -s MODULARIZE=1 -s EXPORT_NAME='createLmdbModule' -s EXPORTED_FUNCTIONS=[\"_mdb_env_create\",\"_mdb_env_open\",\"_mdb_env_close\",\"_mdb_env_set_maxdbs\",\"_mdb_env_set_mapsize\",\"_mdb_env_get_maxreaders\",\"_mdb_env_set_maxreaders\",\"_mdb_env_get_maxkeysize\",\"_mdb_reader_check\",\"_mdb_env_get_flags\",\"_mdb_env_set_flags\",\"_mdb_env_stat\",\"_mdb_env_info\",\"_mdb_env_copy\",\"_mdb_env_copy2\",\"_mdb_env_sync\",\"_mdb_txn_begin\",\"_mdb_txn_commit\",\"_mdb_txn_abort\",\"_mdb_txn_reset\",\"_mdb_txn_renew\",\"_mdb_txn_id\",\"_mdb_dbi_open\",\"_mdb_dbi_close\",\"_mdb_drop\",\"_mdb_stat\",\"_mdb_dbi_flags\",\"_mdb_cmp\",\"_mdb_dcmp\",\"_mdb_get\",\"_mdb_put\",\"_mdb_del\",\"_mdb_cursor_open\",\"_mdb_cursor_close\",\"_mdb_cursor_get\",\"_mdb_cursor_put\",\"_mdb_cursor_del\",\"_mdb_cursor_count\",\"_mdb_cursor_renew\",\"_mdb_strerror\",\"_mdb_version\",\"_malloc\",\"_free\",\"_mkdir\",\"_access\",\"_rmdir\",\"_unlink\",\"_opendir\",\"_readdir\",\"_closedir\"] -s IMPORTED_MEMORY=1 -s ALLOW_MEMORY_GROWTH=1 -s MAXIMUM_MEMORY=2GB -s STACK_SIZE=65536 -s TOTAL_STACK=65536 -s INITIAL_MEMORY=16MB -s FORCE_FILESYSTEM=1 -s FILESYSTEM=1 -s ERROR_ON_UNDEFINED_SYMBOLS=0 --no-entry -DMDB_USE_POSIX_MUTEX=0 -DMDB_USE_ROBUST=0"
)

function compile_lib() {
  echo "Build starting for $2"
  make clean
  if ! eval "$1"
  then
    echo "Build failed for $2"
    exit 1
  fi
  echo "Build succeeded for $2"
  # Determine the output file based on the target
  if [[ "$2" == wasmJs/* ]]; then
    output_file="liblmdb.mjs"
    target_dir="../../../../src/wasmWasiMain/resources/"
  else
    output_file="./liblmdb.so"
    target_dir="../../../../src/jvmMain/resources/native-libs/$2"
  fi

  # Check if the output file exists
  if [ ! -f "$output_file" ]; then
    echo "Output file $output_file not found for $2"
    # Try to find the actual output file
    echo "Looking for alternative output files:"
    ls -la
    # For wasmJs, emscripten might output different files
    if [[ "$2" == wasmJs/* ]]; then
      # Check for common emscripten output files
      for possible_file in liblmdb.js liblmdb.wasm liblmdb.html; do
        if [ -f "./$possible_file" ]; then
          echo "Found $possible_file, using it as output"
          output_file="./$possible_file"
          break
        fi
      done
    fi
  fi

  # Calculate hash and copy the file
  output_hash=$(md5 "$output_file")
  echo "$2 $output_hash"
  build_outputs["$output_hash"]="$2"

  # Create target directory if it doesn't exist
  mkdir -p "$(dirname "$target_dir")"

  # Copy the file to the appropriate resources directory
  cp "$output_file" "$target_dir"
  
  # For ES6 module builds, also copy the WASM file
  if [[ "$output_file" == *.mjs ]]; then
    wasm_file="${output_file%.mjs}.wasm"
    if [ -f "$wasm_file" ]; then
      echo "Also copying $wasm_file"
      cp "$wasm_file" "$target_dir"
    fi
  fi

  # Note: With EXPORT_ES6=1, both .mjs and .wasm files are generated
  sleep 10 
  #seems to be a stateful race condition on the docker run processes so this allows everything to succeed
}

for key in "${!supported_targets[@]}"; do
  compile_lib "${supported_targets[$key]}" $key
done

if [ ${#supported_targets[@]} -eq ${#build_outputs[@]} ]; then
    echo "All builds for lmdb supported targets have succeeded"
else
    echo "Not all supported targets have produced unique output"
fi
