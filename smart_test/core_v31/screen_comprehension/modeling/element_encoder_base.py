import logging
import random

import torch
from torch import nn
from torchvision.models import mobilenet_v3_small
from transformers import BertConfig

from screen_comprehension.modeling.custom_modeling_bert import BertModel

logger = logging.getLogger(__name__)


def get_img_encoder():
    # img_encoder = mobilenet_v3_small(weights='IMAGENET1K_V1')
    img_encoder = mobilenet_v3_small(pretrained=True)
    output_dim = img_encoder.classifier[0].in_features
    img_encoder.classifier = nn.Identity()
    return img_encoder, output_dim


class ElementsEncoderConfig(BertConfig):
    r"""
    This is the configuration class to store the configuration of a :class:`~transformers.ScreenBertModel`. It is used to
    instantiate a ScreenBert model according to the specified arguments, defining the model architecture.
    Configuration objects inherit from :class:`~transformers.BertConfig` and can be used to control the model outputs.
    Read the documentation from :class:`~transformers.BertConfig` for more information.
    Args:
        vocab_size (:obj:`int`, `optional`, defaults to 30522):
            Vocabulary size of the ScreenBert model. Defines the different tokens that can be represented by the
            `inputs_ids` passed to the forward method of :class:`~transformers.ScreenBertModel`.
        hidden_size (:obj:`int`, `optional`, defaults to 768):
            Dimensionality of the encoder layers and the pooler layer.
        num_hidden_layers (:obj:`int`, `optional`, defaults to 12):
            Number of hidden layers in the Transformer encoder.
        num_attention_heads (:obj:`int`, `optional`, defaults to 12):
            Number of attention heads for each attention layer in the Transformer encoder.
        intermediate_size (:obj:`int`, `optional`, defaults to 3072):
            Dimensionality of the "intermediate" (i.e., feed-forward) layer in the Transformer encoder.
        hidden_act (:obj:`str` or :obj:`function`, `optional`, defaults to :obj:`"gelu"`):
            The non-linear activation function (function or string) in the encoder and pooler. If string,
            :obj:`"gelu"`, :obj:`"relu"`, :obj:`"silu"` and :obj:`"gelu_new"` are supported.
        hidden_dropout_prob (:obj:`float`, `optional`, defaults to 0.1):
            The dropout probability for all fully connected layers in the embeddings, encoder, and pooler.
        attention_probs_dropout_prob (:obj:`float`, `optional`, defaults to 0.1):
            The dropout ratio for the attention probabilities.
        max_position_embeddings (:obj:`int`, `optional`, defaults to 512):
            The maximum sequence length that this model might ever be used with. Typically set this to something large
            just in case (e.g., 512 or 1024 or 2048).
        type_vocab_size (:obj:`int`, `optional`, defaults to 2):
            The vocabulary size of the :obj:`token_type_ids` passed into :class:`~transformers.ScreenBertModel`.
        initializer_range (:obj:`float`, `optional`, defaults to 0.02):
            The standard deviation of the truncated_normal_initializer for initializing all weight matrices.
        layer_norm_eps (:obj:`float`, `optional`, defaults to 1e-12):
            The epsilon used by the layer normalization layers.
        gradient_checkpointing (:obj:`bool`, `optional`, defaults to :obj:`False`):
            If True, use gradient checkpointing to save memory at the expense of slower backward pass.
        max_2d_position_embeddings (:obj:`int`, `optional`, defaults to 1024):
            The maximum value that the 2D position embedding might ever used. Typically set this to something large
            just in case (e.g., 1024).
    """
    model_type = "ScreenBert"

    def __init__(
            self,
            vocab_size=30522,
            hidden_size=768,
            sentence_embedding_size=768,
            num_hidden_layers=12,  # changed
            num_attention_heads=12,  # changed
            intermediate_size=3072,
            hidden_act="gelu",
            hidden_dropout_prob=0.1,
            attention_probs_dropout_prob=0.1,
            max_position_embeddings=1024,
            type_vocab_size=1,  # changed, used for segment token
            initializer_range=0.02,
            layer_norm_eps=1e-12,
            pad_token_id=0,
            gradient_checkpointing=False,
            max_2d_position_embeddings=1281,  # changed
            element_vocab_size=10,  # added
            task_dropout_prob=0.1,  # added
            img_embedding_size=3 * 32 * 32,  # added
            padding_idx=0,  # added
            max_timestep=23,  # added
            max_relative_formid=6,
            max_relative_bbox=23,
            relative_embedding_size=1,
            **kwargs
    ):
        super().__init__(
            vocab_size=vocab_size,
            hidden_size=hidden_size,
            num_hidden_layers=num_hidden_layers,
            num_attention_heads=num_attention_heads,
            intermediate_size=intermediate_size,
            hidden_act=hidden_act,
            hidden_dropout_prob=hidden_dropout_prob,
            attention_probs_dropout_prob=attention_probs_dropout_prob,
            max_position_embeddings=max_position_embeddings,
            type_vocab_size=type_vocab_size,
            initializer_range=initializer_range,
            layer_norm_eps=layer_norm_eps,
            pad_token_id=pad_token_id,
            gradient_checkpointing=gradient_checkpointing,
            **kwargs,
        )
        self.max_2d_position_embeddings = max_2d_position_embeddings
        self.element_vocab_size = element_vocab_size  # added
        self.task_dropout_prob = task_dropout_prob  # added
        self.sentence_embedding_size = sentence_embedding_size
        self.img_embedding_size = img_embedding_size  # added
        self.padding_idx = padding_idx  # added
        self.output_attentions = False
        self.max_timestep = max_timestep
        self.max_relative_formid = max_relative_formid
        self.max_relative_bbox = max_relative_bbox
        self.relative_embedding_size = relative_embedding_size


class ElementsEncoderEmbeddings(nn.Module):
    def __init__(self, config, with_img_encoder=True):
        super(ElementsEncoderEmbeddings, self).__init__()

        img_encoder, img_embedding_size = get_img_encoder()
        self.img_project = nn.Sequential(img_encoder, nn.Linear(img_embedding_size, config.hidden_size)) \
            if with_img_encoder else None
        self.txt_project = nn.Linear(config.sentence_embedding_size, config.hidden_size)
        self.el_type_embeddings = nn.Embedding(
            config.element_vocab_size,
            config.hidden_size,
            padding_idx=config.padding_idx,
        )  # padding_idx is for [PAD] token with id 0

        self.x_position_embeddings = nn.Embedding(
            config.max_2d_position_embeddings, config.hidden_size
        )
        self.y_position_embeddings = nn.Embedding(
            config.max_2d_position_embeddings, config.hidden_size
        )
        self.h_position_embeddings = nn.Embedding(
            config.max_2d_position_embeddings, config.hidden_size
        )
        self.w_position_embeddings = nn.Embedding(
            config.max_2d_position_embeddings, config.hidden_size
        )

        # self.LayerNorm is not snake-cased to stick with TensorFlow model variable name and be able to load
        # any TensorFlow checkpoint file
        self.LayerNorm = nn.LayerNorm(config.hidden_size, eps=config.layer_norm_eps)
        self.dropout = nn.Dropout(config.hidden_dropout_prob)

        # self.sep_embedding = nn.Parameter(torch.FloatTensor(1, config.hidden_size))
        self.cls_embedding = nn.Parameter(torch.FloatTensor(1, config.hidden_size))
        nn.init.normal_(self.cls_embedding)
        self.seg_embedding = nn.Embedding(4, config.hidden_size)
        # nn.init.trunc_normal_(self.seg_embedding.weight, std=offical_std, a=-2*offical_std, b=2*offical_std)

    def forward(
            self,
            el_type_ids,
            txt_features,
            bboxes,
            seg_ids,
            attention_mask,
            imgs=None,
    ):
        txt_features = self.txt_project(txt_features)
        if imgs != None:
            B, L, C, H, W = imgs.shape
            imgs = imgs.view(-1, C, H, W)
            img_features = self.img_project(imgs)
            img_features = img_features.view(B, L, -1)
        el_type_embeddings = self.el_type_embeddings(el_type_ids)
        left_position_embeddings = self.x_position_embeddings(bboxes[:, :, 0])
        upper_position_embeddings = self.y_position_embeddings(bboxes[:, :, 1])
        right_position_embeddings = self.x_position_embeddings(bboxes[:, :, 2])
        lower_position_embeddings = self.y_position_embeddings(bboxes[:, :, 3])
        h_position_embeddings = self.h_position_embeddings(
            bboxes[:, :, 3] - bboxes[:, :, 1]
        )
        w_position_embeddings = self.w_position_embeddings(
            bboxes[:, :, 2] - bboxes[:, :, 0]
        )

        embeddings = (
                el_type_embeddings
                + txt_features
                + left_position_embeddings
                + upper_position_embeddings
                + right_position_embeddings
                + lower_position_embeddings
                + h_position_embeddings
                + w_position_embeddings
        )

        if imgs != None:
            embeddings += img_features

        # add seg_embeddings
        seg_embeddings = self.seg_embedding(seg_ids)
        embeddings = embeddings + seg_embeddings

        # concat cls embedding
        B, N, D = embeddings.shape
        cls_embedding = self.cls_embedding.unsqueeze(0).expand(B, -1, -1)
        embeddings = torch.cat([cls_embedding, embeddings], dim=1)

        attention_mask = torch.cat([torch.ones([B, 1], dtype=torch.bool, device=attention_mask.device), attention_mask],
                                   dim=1)

        return embeddings, attention_mask


class ElementsEncoderModel(BertModel):
    r"""
    This is the ScreenBert class, which use the forward function to call.
    Inputs:
        el_type_ids (:obj:`torch.LongTensor`, `required`):
            Shape: [B, config.max_seq_len, ]
            Long int value in [0, 1, 2, 3, 4], where 0 represents padding elements, 1/2/3/4 represents different element type.
        txt_features (:obj:`torch.FloatTensor`, `required`):
            Shape: [B, config.max_seq_len, config.sentence_embedding_size]
            Encoded text features.
        bboxes (:obj:`torch.LongTensor`, `required`):
            Shape: [B, config.max_seq_len, 4]
            Normed bboxes.
        seg_ids (:obj:`torch.LongTensor`, `required`):
            Shape: [B, config.max_seq_len, ]
            Long int value in [0, 1, 2, 3], where 0 represents padding elements, 1/2/3 represents corespponding UI frame.
        imgs (:obj:`torch.FloatTensor`, `optional`, defaults to None):
            Shape: [B, config.max_seq_len, 3, 32, 32]
            Cropped element image patches and resized to 32x32x3.
        attention_mask (:obj:`torch.LongTensor`, `optional`, defaults to None):
            Shape: [B, config.max_seq_len]
            Valid element mark, where 0 represents padding(invalid) elements, 1 represents valid elements.
    Outputs:
        "last_hidden_state":
            Shape: [B, config.max_seq_len, config.hidden_size]
            Representation vector per element.
        "pooler_output":
            Shape: [B, config.hidden_size]
            CLS token, the global representation vector.
    """

    config_class = ElementsEncoderConfig
    pretrained_model_archive_map = {}
    base_model_prefix = "bert"

    def __init__(self, config):
        super(ElementsEncoderModel, self).__init__(config)
        self.embeddings = ElementsEncoderEmbeddings(config)
        self.init_weights()

    def forward(
            self,
            el_type_ids,
            txt_features,
            imgs,
            bboxes,
            seg_ids,
            attention_mask=None,
            token_type_ids=None,
            position_ids=None,
            head_mask=None,
            inputs_embeds=None,
            encoder_hidden_states=None,
            encoder_attention_mask=None,
    ):
        if attention_mask is None:
            attention_mask = torch.ones_like(el_type_ids)
        if token_type_ids is None:
            token_type_ids = torch.zeros_like(el_type_ids)

        embedding_output, attention_mask = self.embeddings(
            el_type_ids=el_type_ids,
            txt_features=txt_features,
            imgs=imgs,
            bboxes=bboxes,
            seg_ids=seg_ids,
            attention_mask=attention_mask,
            # position_ids=position_ids,
            # token_type_ids=token_type_ids,
        )  # seq_len will + 1

        # We create a 3D attention mask from a 2D tensor mask.
        # Sizes are [batch_size, 1, 1, to_seq_length]
        # So we can broadcast to [batch_size, num_heads, from_seq_length, to_seq_length]
        # this attention mask is more simple than the triangular masking of causal attention
        # used in OpenAI GPT, we just need to prepare the broadcast dimension here.
        extended_attention_mask = attention_mask.unsqueeze(1).unsqueeze(2)

        # Since attention_mask is 1.0 for positions we want to attend and 0.0 for
        # masked positions, this operation will create a tensor which is 0.0 for
        # positions we want to attend and -10000.0 for masked positions.
        # Since we are adding it to the raw scores before the softmax, this is
        # effectively the same as removing these entirely.
        extended_attention_mask = extended_attention_mask.to(
            dtype=next(self.parameters()).dtype
        )  # fp16 compatibility
        extended_attention_mask = (1.0 - extended_attention_mask) * -10000.0

        # Prepare head mask if needed
        # 1.0 in head_mask indicate we keep the head
        # attention_probs has shape bsz x n_heads x N x N
        # input head_mask has shape [num_heads] or [num_hidden_layers x num_heads]
        # and head_mask is converted to shape [num_hidden_layers x batch x num_heads x seq_length x seq_length]
        if head_mask is not None:
            if head_mask.dim() == 1:
                head_mask = (
                    head_mask.unsqueeze(0).unsqueeze(0).unsqueeze(-1).unsqueeze(-1)
                )
                head_mask = head_mask.expand(
                    self.config.num_hidden_layers, -1, -1, -1, -1
                )
            elif head_mask.dim() == 2:
                head_mask = (
                    head_mask.unsqueeze(1).unsqueeze(-1).unsqueeze(-1)
                )  # We can specify head_mask for each layer
            head_mask = head_mask.to(
                dtype=next(self.parameters()).dtype
            )  # switch to fload if need + fp16 compatibility
        else:
            head_mask = [None] * self.config.num_hidden_layers

        encoder_outputs = self.encoder(
            embedding_output,
            extended_attention_mask,
            head_mask=head_mask,
            output_attentions=False,
            output_hidden_states=True,
        )  # return tuple of len 2, last hidden state and all hidden states tuple
        sequence_output = encoder_outputs[0]
        pooled_output = self.pooler(sequence_output)

        return {
            "last_hidden_state": sequence_output[:, 1:],  # not include cls token
            "pooler_output": pooled_output,
        }


if __name__ == "__main__":
    config = ElementsEncoderConfig()
    embedding = ElementsEncoderEmbeddings(config)
    tensor_sample = torch.randint(0, 960, (16, 256, 960))
    print(tensor_sample[1])
    random_list = random.sample(range(0, 256), int(256 * 0.15))
    empty_embedding = torch.zeros(960, dtype=torch.float32)
    for i in range(tensor_sample.shape[0]):
        for number in random_list:
            tensor_sample[i, number] = empty_embedding
            print(tensor_sample[i, number])
            # print(tensor_sample[i, number])
