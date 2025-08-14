
<img width="100" height="100" align="left" style="float: left; margin: 0 10px 0 0;" alt="lb++" src="src/main/resources/assets/minecraft/minusbounce/big.png">

# MinusBeta (MinusBounce unreleased)
A free forge hacked-client for Minecraft supporting version 1.8.9

Website: https://minusbounce.lol \
Discord: https://discord.minusbounce.lol

### Notes:
- MinusBounce có 2 phiên bản, bao gồm MinusBounce (đã release và discontinued từ trước) và MinusBeta (một phiên bản khác của MinusBounce, bao gồm những bypass, module mới)
- Khi mình mới vào MinusMC, mình đã có dự định đối với MinusBeta là như vậy. Nhưng vì những chuyện xảy ra sau đó, MinusBounce đã discontinued và đã removed khỏi GitHub. Cũng vì vậy mà MinusBeta cũng discontinued ngay sau đó.
- Mặc dù client chưa được tốt, đôi khi còn nhiều bugs, flags, nhưng đấy là những thứ tốt nhất mình có thể làm được trong khoảng thời gian đấy. Source cũng đã clean, nhìn cũng không đến nỗi mù mắt. Không GPT code, hoàn toàn do chính tay mình tạo nên.
- Cũng là vì do bản kế nhiệm của MinusBounce - MinusBounce Reborn. Source quá nhiều lỗi, đa số là GPT code, cũng đã broke luôn source cũ (i think so). Nên đối với người tạo ra, không khác gì 1 phát đấm thẳng vào mồm người đó.
- Những ngày phát triển phiên bản này, là những ngày vui vẻ nhất trong những năm mình học code gradle, java và kotlin. Khi mình cố gắng học thêm cách làm bypass, module, có khi thức đến sáng chỉ để vào server test anticheat. Một ngày gần 20 commits, khá là mệt nhưng khi thấy client có thể hvh, bypass scaffold,... Thì đấy là niềm vui lớn nhất, mang thêm nhiều động lực để mình tiếp tục. Nhưng khi nhìn thấy tâm huyết của chính mình, cùng với đó những đoạn code mình tạo ra được patch sơ sài bởi GPT, không khác gì nồi cám lợn, trong thân tâm mình cảm thấy rất bực bội.
- Nếu những người phát triển MinusBounce Reborn có đọc được đoạn này, hãy học code 1 cách cẩn thận. Đừng để tâm huyết của mình đi theo là những dòng code do chatGPT viết ra. Hãy clean, refactor nó, phát triển nó thành một phiên bản tốt hơn bây giờ, chứ không phải chỉ copy và paste từ promp ra. Nó không mang lại điều gì cả, thậm chí còn gây tức điên mình lên. Nếu có thể làm được, đấy là niềm vui còn lớn hơn cả lúc mình phát triển phiên bản này.
- MinusBeta bao gồm những bypass mà mình chưa merge vào bên git main, cũng là những thứ mình bỏ công sức ra để làm, mặc dù logic đã cũ, đôi khi còn skid, nhưng mình đã cố gắng làm để nó được tốt nhất.
- Mang nhiều hi vọng của mình, cũng là vì source code đã leaked ra ngoài và được continue bởi [MinusBounce-Reborn](https://github.com/MinusMC/MinusBounce-Reborn). Vì những lí do trên, mình quyết định rằng là sẽ public source code chuẩn của MinusBeta.
- Nên, hãy coi MinusBeta là phiên bản cuối cùng của MinusBouce, là di sản mà mình để lại của MinusBounce.
- MinusBeta - Di sản cuối cùng của MinusBounce (by fmcpe).
- Best regards, fmcpe (14/8/25).

### Issues
If you notice any bugs or missing features, you can let us know by opening an issue [here](https://github.com/MinusMCNetwork/MinusBounce/issues).

### License
This project is subject to the [GNU General Public License v3.0](LICENSE). This does only apply for source code located directly in this clean repository. During the development and compilation process, additional source code may be used to which we have obtained no rights. Such code is not covered by the GPL license.

For those who are unfamiliar with the license, here is a summary of its main points. This is by no means legal advice nor legally binding.

You are allowed to
- use
- share
- modify

this project entirely or partially for free and even commercially. However, please consider the following:

- **You must disclose the source code of your modified work and the source code you took from this project. This means you are not allowed to use code from this project (even partially) in a closed-source (or even obfuscated) application.**
- **Your modified application must also be licensed under the GPL** 

Do the above and share your source code with everyone; just like we do.

### Developement

1. Clone the repository:

    ```bash
    git clone https://github.com/MinusMCNetwork/MinusBounce.git
    ```
   
2. Navigate to the project directory:

    ```bash
    cd MinusBounce
    ```

3. Depending on which IDE you are using execute either of the following commands:
   - For IntelliJ: `gradlew setupDevWorkspace idea genIntellijRuns build`
   - For Eclipse: `gradlew setupDevWorkspace eclipse build`
