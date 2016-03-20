package org.linphone;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import org.linphone.setup.SetupActivity;

public class LegalRelease extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LegalRelease.this);
        boolean hasAcceptedLegalRelease = prefs.getBoolean("accepted_legal_release", false);
        if(hasAcceptedLegalRelease){
            this.finish();
        }
        setContentView(R.layout.activity_legal_release);
        final TextView legalTextView = ((TextView)findViewById(R.id.legalTextView));
        final ScrollView scrollView = ((ScrollView)findViewById(R.id.legalScrollView));

        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (scrollView.getChildCount() > 0) {
                    View view = (View) scrollView.getChildAt(scrollView.getChildCount() - 1);
                    int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
                    if (diff <= 0) {
                        ((Button) findViewById(R.id.acceptLegalButton)).setEnabled(true);
                    }
                }
            }
        });

        legalTextView.setText("\n" +
                "\n" +
                "\t12/29/2015 DRAFT\n" +
                "ENGLISH\n" +
                "\n" +
                "IMPORTANT: BY USING THE ACE APP YOU ARE AGREEING TO BE BOUND BY THE TERMS\n" +
                "AND CONDITIONS OF THIS SOFTWARE LICENSE AGREEMENT.\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "VTCSECURE, LLC\n" +
                "SOFTWARE LICENSE AGREEMENT\n" +
                "Single Use License\n" +
                "\n" +
                "BEFORE YOU CLICK ON THE \"I AGREE\" BUTTON BELOW AND ACCESS AND/OR USE THE SOFTWARE (AS DEFINED BELOW) OR DOWNLOAD ANY SOFTWARE UPDATE (AS DEFINED BELOW), PLEASE CAREFULLY READ THIS SOFTWARE LICENSE AGREEMENT (\"LICENSE\"). BY USING THE ACE APP SOFTWARE OR DOWNLOADING ANY SOFTWARE UPDATE, AS APPLICABLE, YOU ARE AGREEING TO BE BOUND BY THE TERMS AND CONDITIONS OF THIS LICENSE. IF YOU DO NOT AGREE TO THE TERMS OF THIS LICENSE, DO NOT USE THE ACE APP SOFTWARE OR DOWNLOAD ANY SOFTWARE UPDATE. BY CLICKING THE \"I AGREE” BUTTON BELOW, YOU ACKNOWLEDGE THAT YOU HAVE READ, UNDERSTAND, AND AGREE TO ALL OF THE TERMS AND CONDITIONS OF THE LICENSE. VTCSECURE RESERVES THE RIGHT, WITH OR WITHOUT NOTICE, TO AMEND OR MODIFY THIS LICENSE AND YOU AGREE TO BE BOUND BY ANY SUCH AMENDMENT OR MODIFICATION. MODIFICATIONS OR AMENDMENTS TO THIS LICENSE SHALL BE EFFECTIVE AT THE TIME THEY ARE POSTED ON THE VTCSECURE WEBSITE. THIS LICENSE IS NOT DEPENDENT ON ANY FUTURE FUNCTIONALITY OR FEATURES.\n" +
                "\n" +
                "\t•\tGeneral.\n" +
                "\n" +
                "\t•\tThe VTCSecure, LLC (“VTCSecure”) software (including Boot ROM code, embedded software and third party software), documentation, interfaces, content, fonts and any data that came with your ACE App (\"Original Software\"), as may be updated or replaced from time to time by feature enhancements, software updates, and/or system restore software provided by VTCSecure (\"Software Updates\"), whether in read only memory, on any other media or in any other form (the Original Software and Software Updates are collectively referred to as the “Software\") are licensed, not sold, to you by VTCSecure for use only under the terms of this License. VTCSecure retains ownership of the Software and reserves all rights not expressly granted to you. You agree that the terms of this License will apply to the Software and any use by you of the Software.\n" +
                "\n" +
                "\t•\tVTCSecure, at its discretion, may make available future Software Updates for the ACE App. The Software Updates, if any, may not necessarily include all existing software features. The terms of this License will govern any Software Updates provided by VTCSecure that replace and/or supplement the Original Software product, unless such Software Update\n" +
                "is accompanied by a separate license, in which case the terms of that license will govern.\n" +
                "\f\n" +
                "\n" +
                "\t•\tPermitted License Uses and Restrictions.\n" +
                "\n" +
                "\t•\tSubject to the terms and conditions of this License, you are granted a limited, revocable, non-assignable, non-sublicensable, non-exclusive license to use the Software on a single device. Except as permitted in Section 2(b) below, and except as provided in a separate written agreement between you and VTCSecure, this License does not allow the Software to exist on more than one device at a time, and you may not distribute or make the Software available over a network where it could be used by multiple devices at the same time. This License does not grant you any rights to use VTCSecure proprietary interfaces and/or any other intellectual property in the design, development, manufacture, licensing or distribution of third party devices and accessories, or third party software applications, for use with the ACE App. Some of those rights are available under separate licenses from VTCSecure.\n" +
                "\n" +
                "\t•\tSubject to the terms and conditions of this License, you are granted a limited, revocable, non-assignable, non-sublicensable, non-exclusive license to download Software Updates that may be made available by VTCSecure to update or restore the Software on any device you own or control. This License does not allow you to update or restore any device that you do not control or own, and you may not distribute or make the Software Updates available over a network where they could be used by multiple devices at the same time.\n" +
                "\n" +
                "\t•\tYou may not, and you agree not to or enable others to, copy (except as expressly permitted by this License), decompile, reverse engineer, disassemble, attempt to derive the source code of, decrypt, modify, or create derivative works of the Software or any services provided by the Software or any part thereof (except as and only to the extent any foregoing restriction is prohibited by applicable law or by licensing terms governing use of open-source components that may be included with the Software).\n" +
                "\n" +
                "\t•\tThe Software may be used to reproduce materials so long as such use is limited to reproduction of non-copyrighted materials, materials in which you own the copyright, or materials you are authorized or legally permitted to reproduce. Title and intellectual property rights in and to any content displayed by, stored on or accessed through the ACE App belong to the respective content owner. Such content may be protected by copyright or other intellectual property laws and treaties, and may be subject to terms of use of the third party providing such content. Except as otherwise provided herein, this License does not grant you any rights to use such content nor does it guarantee that such content will continue to be available to you.\n" +
                "\n" +
                "(e) You agree to use the Software and the Services (as defined in Section 5 below) in compliance with all applicable laws, including local laws of the country or region in which you reside or in which you download or use the Software and Services. Features of the Software and the Services may not be available in all languages or regions, some features may vary by region, and some may be restricted or unavailable from your service provider.\n" +
                "\f\n" +
                "\n" +
                "(f) Using the ACE App in some circumstances can distract you and may cause a dangerous situation (for example, avoid typing text messages while driving a car or using headphones while riding a bicycle). By using the ACE App you agree that you are responsible for observing rules that prohibit or restrict the use of mobile phones or headphones (for example, the requirement to use hands-free options for making calls when driving).\n" +
                "\n" +
                "\t•\tTransfer. You may not rent, lease, lend, sell, redistribute, or sublicense the Software.\n" +
                "\n" +
                "\t•\tConsent to Use of Data. The Software may require information from your device. You can find more information on which features send information to VTCSecure, what information they send and how it may be used, when you turn on or use the features. At all times your information will be treated in accordance with VTCSecure's Privacy Policy, which can be viewed at www.VTCSecure.com.\n" +
                "\n" +
                "\t•\tServices.\n" +
                "\n" +
                "\t•\tThe Software may enable access to third party services and web sites (collectively and individually, \"Services\"). Such Services may not be available in all languages or in all countries. Use of these Services requires Internet access and use of certain Services may require a user name and password, and may require you to accept additional terms and may be subject to additional fees.\n" +
                "\t•\tVTCSecure does not guarantee the availability, accuracy, completeness, reliability, or timeliness of location data or any other data displayed by the Software or Services.  Location data, whether provided by the Software or Services, is not intended to be relied upon in situations where precise location information is needed or where erroneous, inaccurate, time-delayed or incomplete location data may lead to death, personal injury, property or environmental damage.\n" +
                "\t•\tTo the extent that you upload any content through the use of the Services, you represent that you own all rights in, or have authorization or are otherwise legally permitted to upload, such content and that such content does not violate any terms of service applicable to the Services. You agree that the Services contain proprietary content, information and material that is owned by the third party service provider(s), the site owner and/or their licensors, and is protected by applicable intellectual property and other laws, including but not limited to copyright. You agree that you will not use such proprietary content, information or materials other than for permitted use of the Software or in any manner that is inconsistent with the terms of this License or that infringes any intellectual property rights of a third party or VTCSecure. No portion of the Software may be reproduced in any form or by any means. You agree not to modify, rent, lease, loan, sell, distribute, or create derivative works based on the Services, in any manner, and you shall not exploit the Services in any unauthorized way whatsoever, including but not limited to, using the Services to transmit any computer viruses, worms, trojan horses or other malware, or by trespass or burdening network capacity. You further agree not to use the Services in any manner to harass, abuse, stalk, threaten, defame or otherwise infringe or violate the rights of any other party, and that VTCSecure is not in any way responsible for any such use by you, nor for any harassing, threatening, defamatory, offensive, infringing or illegal messages or transmissions that you may receive as a result of using the Software and/or any of the Services.\n" +
                "\t•\tIn addition, Services and any third party materials that may be accessed, linked to or displayed on the Software are not available in all languages or in all countries or regions. VTCSecure makes no representation that such Services and/or third party materials are appropriate or available for use in any particular location. To the extent you choose to use or access such Services and/or third party materials, you do so at your own initiative and risk and are responsible for compliance with all applicable laws, including but not limited to applicable local laws and privacy and data collection laws. Sharing or transmitting photos through the Software may cause metadata, including photo location data, to be transmitted with the photos. VTCSecure and its licensors reserve the right to change, suspend, remove, or disable access to any Services at any time without notice. In no event will VTCSecure be liable for the removal of or disabling of access to any such Services. VTCSecure in its sole discretion may also impose limits on the use of or access to certain Services without notice or liability.\n" +
                "\n" +
                "\t•\tTermination. This License is effective until terminated. Your rights under this License will terminate automatically or otherwise cease to be effective without notice from VTCSecure if you fail to comply with any term(s) or condition(s) of this License. Upon the termination of this License, you shall immediately cease all use of the Software. Sections 4, 5, 6, 7, 8, 11 and 12 of this License shall survive any such termination for any reason.\n" +
                "\n" +
                "\t•\t911 DISCLAIMER.  You acknowledge and agree that 911 Service will not be available when making any calls using the Software. 911 Service enables users to communicate with emergency services personnel by dialing 911 on a wireline, wireless, and/or mobile telephone. You acknowledge that you are  responsible for making arrangements to ensure that you will have access to 911 Service without relying on the Software to do so.  You shall also inform any other user of the Software of the unavailability of traditional 911 or E911 dialing and access to 911 Service or emergency personnel. You further agree that VTCSecure shall have no liability whatsoever with respect to any attempt by you or any other user to access any 911 Service using the Software.\n" +
                "\n" +
                "\t•\tDisclaimer of   Warranties.\n" +
                "\t•\tIf you are a customer who is a consumer (someone who uses the Software outside of your trade, business or profession), you may have legal rights in your country of residence which would prohibit the following limitations from applying to you, and where prohibited they will not apply to you. To find out more about rights, you should contact a local consumer advice organization.\n" +
                "\n" +
                "\n" +
                "\t•\tYOU EXPRESSLY ACKNOWLEDGE AND AGREE THAT, TO THE EXTENT PERMITTED BY APPLICABLE LAW, USE OF THE SOFTWARE AND ANY SERVICES PERFORMED BY OR ACCESSED THROUGH THE SOFTWARE IS AT YOUR SOLE RISK AND THAT THE ENTIRE RISK AS TO SATISFACTORY QUALITY, PERFORMANCE, ACCURACY AND EFFORT IS WITH YOU.\n" +
                "\n" +
                "\t•\tTO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, THE SOFTWARE AND SERVICES ARE PROVIDED \"AS IS\" AND \"AS AVAILABLE\", WITH ALL FAULTS AND WITHOUT WARRANTY OF ANY KIND, AND VTCSECURE AND VTCSECURE'S LICENSORS (COLLECTIVELY REFERRED TO AS \"VTCSECURE\" FOR THE PURPOSES OF SECTIONS 7 AND 8) HEREBY DISCLAIM ALL WARRANTIES AND CONDITIONS WITH RESPECT TO THE SOFTWARE AND SERVICES, EITHER EXPRESS, IMPLIED OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES AND/OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, ACCURACY, QUIET ENJOYMENT, AND NON-INFRINGEMENT OF THIRD PARTY RIGHTS.\n" +
                "\n" +
                "\t•\tVTCSECURE DOES NOT WARRANT AGAINST INTERFERENCE WITH YOUR ENJOYMENT OF THE SOFTWARE AND SERVICES, THAT THE FUNCTIONS CONTAINED IN, OR SERVICES PERFORMED OR PROVIDED BY, THE SOFTWARE WILL MEET YOUR REQUIREMENTS, THAT THE OPERATION OF THE SOFTWARE AND SERVICES WILL BE UNINTERRUPTED OR ERROR-FREE, THAT THE SOFTWARE AND/OR ANY SERVICE WILL CONTINUE TO BE MADE AVAILABLE, THAT DEFECTS IN THE SOFTWARE OR SERVICES WILL BE CORRECTED, OR THAT THE SOFTWARE WILL BE COMPATIBLE OR WORK WITH ANY THIRD PARTY SOFTWARE, APPLICATIONS OR THIRD PARTY SERVICES. INSTALLATION OF THE SOFTWARE MAY AFFECT THE AVAILABILITY AND USABILITY OF THIRD PARTY SOFTWARE, APPLICATIONS OR THIRD PARTY SERVICES, AS WELL AS VTCSECURE PRODUCTS AND SERVICES.\n" +
                "\n" +
                "\t•\tYOU FURTHER ACKNOWLEDGE THAT THE SOFTWARE AND SERVICES ARE NOT INTENDED OR SUITABLE FOR USE IN SITUATIONS OR ENVIRONMENTS WHERE THE FAILURE OR TIME DELAYS OF, OR ERRORS OR INACCURACIES IN, THE CONTENT, DATA OR INFORMATION PROVIDED BY THE SOFTWARE OR SERVICES COULD LEAD TO DEATH, PERSONAL INJURY, PROPERTY DAMAGE, OR SEVERE PHYSICAL OR ENVIRONMENTAL DAMAGE, INCLUDING WITHOUT LIMITATION THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, LIFE SUPPORT OR WEAPONS SYSTEMS.\n" +
                "\n" +
                "\t•\tNO ORAL OR WRITTEN INFORMATION OR ADVICE GIVEN BY VTCSECURE OR A VTCSECURE AUTHORIZED REPRESENTATIVE SHALL CREATE ANY WARRANTY. IF THE SOFTWARE AND/OR ANY SERVICES ARE  DEFECTIVE, YOU ASSUME THE ENTIRE COST AND RISK OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OF IMPLIED WARRANTIES OR LIMITATIONS ON APPLICABLE STATUTORY RIGHTS OF A CONSUMER, SO THE ABOVE EXCLUSION AND LIMITATIONS MAY NOT APPLY TO YOU.\n" +
                "\n" +
                "\t•\tLimitation of Liability. TO THE EXTENT NOT PROHIBITED BY APPLICABLE LAW, IN NO EVENT SHALL VTCSECURE, ITS OFFICERS, PRINCIPALS, DIRECTORS, EMPLOYEES,  CONTRACTORS, AND AGENTS, BE LIABLE FOR ANY PERSONAL INJURY (INCLUDING DEATH), PROPERTY DAMAGE, OR ANY INCIDENTAL, SPECIAL, INDIRECT, OR CONSEQUENTIAL DAMAGES WHATSOEVER, INCLUDING, WITHOUT LIMITATION, ANY DAMAGES FOR LOSS OF REVENUE OR PROFITS, CORRUPTION OR LOSS OF ANY DATA, FAILURE TO TRANSMIT OR RECEIVE ANY DATA, BUSINESS INTERRUPTION OR ANY OTHER DAMAGES OR LOSSES, ARISING OUT OF OR RELATED TO YOUR USE OR INABILITY TO USE THE SOFTWARE AND/OR SERVICES OR ANY THIRD PARTY SOFTWARE OR APPLICATIONS IN CONJUNCTION WITH THE SOFTWARE AND/OR SERVICES, HOWEVER CAUSED, REGARDLESS OF THE THEORY OF LIABILITY (WHETHER IN CONTRACT, TORT OR OTHERWISE) AND EVEN IF VTCSECURE HAS BEEN ADVISED OF THE POSSIBILITY OF ANY SUCH DAMAGES. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR LIMITATION OF LIABILITY FOR PERSONAL INJURY, OR OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THIS LIMITATION MAY NOT APPLY TO YOU. In no event shall VTCSecure's total liability to you for any and all damages (other than as may be required by applicable law in cases involving personal injury) exceed the amount of two hundred and fifty dollars (U.S.$250.00). No action against VTCSecure arising out of this Agreement may be brought by you more than one (1) year after the cause of action or claim for damages has arisen. This Section 8 will apply to the maximum extent permitted under applicable law. The foregoing limitations will apply even if the above stated remedy fails of its essential purpose.\n" +
                "\n" +
                "\t•\tExport Control. You may not use or otherwise export or re-export the Software except as authorized by United States law and the laws of the jurisdiction(s) in which the Software was obtained. In particular, but without limitation, the Software may not be exported or re-exported (a) into any U.S. embargoed countries or (b) to anyone on the U.S. Treasury Department's list of Specially Designated Nationals or the U.S. Department of Commerce Denied Person’s List or Entity List or any other restricted party lists. By using the Software, you represent and warrant that you are not located in any such country or on any such list. You also agree that you will not use the Software for any purposes prohibited by United States law, including, without limitation, the development, design, manufacture or production of missiles, nuclear, chemical or biological weapons.\n" +
                "\n" +
                "\t•\tControlling Law and Severability. This License will be governed by and construed in accordance with the laws of the State of Florida, excluding its conflict of law principles. This License shall not be governed by the United Nations Convention on Contracts for the International Sale of Goods, the application of which is expressly excluded.  If, for any reason, a court of competent jurisdiction finds any provision of this License, or portion thereof, to be unenforceable, the remainder of this License shall continue in full force and effect.\n" +
                "\n" +
                "11. \tComplete Agreement; Governing Language; Waiver. This License constitutes the entire agreement between you and VTCSecure relating to the Software and Services supersedes all prior or contemporaneous understandings regarding such subject matter. This License will be construed and interpreted fairly, in accordance with the plain meaning of its terms, and there will be no presumption or inference against the party drafting this License in construing or interpreting any of the provisions contained in this License. As set forth above, VTCSecure reserves the right to amend or modify this License by posting such amendment or modification on the VTCSecure website, and you agree to be bound by any such amendment or modification. Except as otherwise stated above, no amendment to or modification of this License will be binding unless in writing and signed by VTCSecure. Any translation of this License is done for local requirements and in the event of a dispute between the English and any non-English versions, the English version of this License shall govern, to the extent not prohibited by local law in your jurisdiction. No delay in exercising any right or remedy will operate as a waiver of such right or remedy or any other right or remedy. A waiver on one occasion will not be construed as a waiver of any right or remedy on any future occasion.\n" +
                "____ I ACCEPT THE FOREGOING LICENSE TERMS AND CONDITIONS.\n");

        ((Button)findViewById(R.id.acceptLegalButton)).setEnabled(true);
        ((Button)findViewById(R.id.acceptLegalButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LegalRelease.this);
                prefs.edit().putBoolean("accepted_legal_release", true).commit();
                startActivity(new Intent().setClass(LegalRelease.this, SetupActivity.class));
                LegalRelease.this.finish();
            }
        });

        ((Button)findViewById(R.id.declineLegalButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                if (currentapiVersion >= Build.VERSION_CODES.JELLY_BEAN){
                    api14AppClose();
                } else{
                    LegalRelease.this.finish();
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void api14AppClose(){
        LegalRelease.this.finishAffinity();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.JELLY_BEAN){
            api14AppClose();
        } else{
            LegalRelease.this.finish();
        }
    }
}

